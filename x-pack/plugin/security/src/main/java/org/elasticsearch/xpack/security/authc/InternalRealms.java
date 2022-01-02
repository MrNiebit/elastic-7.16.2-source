/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.security.authc;

import org.elasticsearch.bootstrap.BootstrapCheck;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.env.Environment;
import org.elasticsearch.license.LicensedFeature;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.core.security.authc.Realm;
import org.elasticsearch.xpack.core.security.authc.RealmConfig;
import org.elasticsearch.xpack.core.security.authc.RealmSettings;
import org.elasticsearch.xpack.core.security.authc.esnative.NativeRealmSettings;
import org.elasticsearch.xpack.core.security.authc.file.FileRealmSettings;
import org.elasticsearch.xpack.core.security.authc.kerberos.KerberosRealmSettings;
import org.elasticsearch.xpack.core.security.authc.ldap.LdapRealmSettings;
import org.elasticsearch.xpack.core.security.authc.oidc.OpenIdConnectRealmSettings;
import org.elasticsearch.xpack.core.security.authc.pki.PkiRealmSettings;
import org.elasticsearch.xpack.core.security.authc.saml.SamlRealmSettings;
import org.elasticsearch.xpack.core.ssl.SSLService;
import org.elasticsearch.xpack.security.Security;
import org.elasticsearch.xpack.security.authc.esnative.NativeRealm;
import org.elasticsearch.xpack.security.authc.esnative.NativeUsersStore;
import org.elasticsearch.xpack.security.authc.esnative.ReservedRealm;
import org.elasticsearch.xpack.security.authc.file.FileRealm;
import org.elasticsearch.xpack.security.authc.kerberos.KerberosRealm;
import org.elasticsearch.xpack.security.authc.ldap.LdapRealm;
import org.elasticsearch.xpack.security.authc.oidc.OpenIdConnectRealm;
import org.elasticsearch.xpack.security.authc.pki.PkiRealm;
import org.elasticsearch.xpack.security.authc.saml.SamlRealm;
import org.elasticsearch.xpack.security.authc.support.RoleMappingFileBootstrapCheck;
import org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore;
import org.elasticsearch.xpack.security.support.SecurityIndexManager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides a single entry point into dealing with all standard XPack security {@link Realm realms}.
 * This class does not handle extensions.
 *
 * @see Realms for the component that manages configured realms (including custom extension realms)
 */
public final class InternalRealms {

    static final String RESERVED_TYPE = ReservedRealm.TYPE;
    static final String NATIVE_TYPE = NativeRealmSettings.TYPE;
    static final String FILE_TYPE = FileRealmSettings.TYPE;
    static final String LDAP_TYPE = LdapRealmSettings.LDAP_TYPE;
    static final String AD_TYPE = LdapRealmSettings.AD_TYPE;
    static final String PKI_TYPE = PkiRealmSettings.TYPE;
    static final String SAML_TYPE = SamlRealmSettings.TYPE;
    static final String OIDC_TYPE = OpenIdConnectRealmSettings.TYPE;
    static final String KERBEROS_TYPE = KerberosRealmSettings.TYPE;

    private static final Set<String> BUILTIN_TYPES = Sets.newHashSet(NATIVE_TYPE, FILE_TYPE);

    /**
     * The map of all <em>licensed</em> internal realm types to their licensed feature
     */
    private static final Map<String, LicensedFeature.Persistent> LICENSED_REALMS = org.elasticsearch.core.Map.ofEntries(
        org.elasticsearch.core.Map.entry(AD_TYPE, Security.AD_REALM_FEATURE),
        org.elasticsearch.core.Map.entry(LDAP_TYPE, Security.LDAP_REALM_FEATURE),
        org.elasticsearch.core.Map.entry(PKI_TYPE, Security.PKI_REALM_FEATURE),
        org.elasticsearch.core.Map.entry(SAML_TYPE, Security.SAML_REALM_FEATURE),
        org.elasticsearch.core.Map.entry(KERBEROS_TYPE, Security.KERBEROS_REALM_FEATURE),
        org.elasticsearch.core.Map.entry(OIDC_TYPE, Security.OIDC_REALM_FEATURE)
    );

    /**
     * The set of all <em>internal</em> realm types, excluding {@link ReservedRealm#TYPE}
     * @deprecated Use of this method (other than in tests) is discouraged.
     */
    @Deprecated
    public static Collection<String> getConfigurableRealmsTypes() {
        return org.elasticsearch.core.Set.copyOf(Sets.union(BUILTIN_TYPES, LICENSED_REALMS.keySet()));
    }

    static boolean isInternalRealm(String type) {
        return RESERVED_TYPE.equals(type) || BUILTIN_TYPES.contains(type) || LICENSED_REALMS.containsKey(type);
    }

    static boolean isBuiltinRealm(String type) {
        return BUILTIN_TYPES.contains(type);
    }

    /**
     * @return The licensed feature for the given realm type, or {@code null} if the realm does not require a specific license type
     * @throws IllegalArgumentException if the provided type is not an {@link #isInternalRealm(String) internal realm}
     */
    @Nullable
    static LicensedFeature.Persistent getLicensedFeature(String type) {
        if (Strings.isNullOrEmpty(type)) {
            throw new IllegalArgumentException("Empty realm type [" + type + "]");
        }
        if (type.equals(RESERVED_TYPE) || isBuiltinRealm(type)) {
            return null;
        }
        final LicensedFeature.Persistent feature = LICENSED_REALMS.get(type);
        if (feature == null) {
            throw new IllegalArgumentException("Unsupported realm type [" + type + "]");
        }
        return feature;
    }

    /**
     * Creates {@link Realm.Factory factories} for each <em>internal</em> realm type.
     * This excludes the {@link ReservedRealm}, as it cannot be created dynamically.
     *
     * @return A map from <em>realm-type</em> to <code>Factory</code>
     */
    public static Map<String, Realm.Factory> getFactories(
        ThreadPool threadPool,
        ResourceWatcherService resourceWatcherService,
        SSLService sslService,
        NativeUsersStore nativeUsersStore,
        NativeRoleMappingStore nativeRoleMappingStore,
        SecurityIndexManager securityIndex
    ) {

        Map<String, Realm.Factory> map = new HashMap<>();
        map.put(FileRealmSettings.TYPE, config -> new FileRealm(config, resourceWatcherService, threadPool));
        map.put(NativeRealmSettings.TYPE, config -> {
            final NativeRealm nativeRealm = new NativeRealm(config, nativeUsersStore, threadPool);
            securityIndex.addStateListener(nativeRealm::onSecurityIndexStateChange);
            return nativeRealm;
        });
        map.put(
            LdapRealmSettings.AD_TYPE,
            config -> new LdapRealm(config, sslService, resourceWatcherService, nativeRoleMappingStore, threadPool)
        );
        map.put(
            LdapRealmSettings.LDAP_TYPE,
            config -> new LdapRealm(config, sslService, resourceWatcherService, nativeRoleMappingStore, threadPool)
        );
        map.put(PkiRealmSettings.TYPE, config -> new PkiRealm(config, resourceWatcherService, nativeRoleMappingStore));
        map.put(SamlRealmSettings.TYPE, config -> SamlRealm.create(config, sslService, resourceWatcherService, nativeRoleMappingStore));
        map.put(KerberosRealmSettings.TYPE, config -> new KerberosRealm(config, nativeRoleMappingStore, threadPool));
        map.put(
            OpenIdConnectRealmSettings.TYPE,
            config -> new OpenIdConnectRealm(config, sslService, nativeRoleMappingStore, resourceWatcherService)
        );
        return Collections.unmodifiableMap(map);
    }

    private InternalRealms() {}

    public static List<BootstrapCheck> getBootstrapChecks(final Settings globalSettings, final Environment env) {
        final Set<String> realmTypes = Sets.newHashSet(LdapRealmSettings.AD_TYPE, LdapRealmSettings.LDAP_TYPE, PkiRealmSettings.TYPE);
        final List<BootstrapCheck> checks = RealmSettings.getRealmSettings(globalSettings)
            .keySet()
            .stream()
            .filter(id -> realmTypes.contains(id.getType()))
            .map(id -> new RealmConfig(id, globalSettings, env, null))
            .map(RoleMappingFileBootstrapCheck::create)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return checks;
    }

}
