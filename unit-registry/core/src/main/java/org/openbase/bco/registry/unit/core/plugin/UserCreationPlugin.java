package org.openbase.bco.registry.unit.core.plugin;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import rst.domotic.unit.user.UserConfigType.UserConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class UserCreationPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> {

    public static final String DEFAULT_ADMIN_USERNAME_AND_PASSWORD = "admin";
    public static final String BCO_USERNAME = "BCO";

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UserCreationPlugin.class);

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> userUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupConfigRegistry;

    public UserCreationPlugin(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> userUnitConfigRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupConfigRegistry) {
        this.userUnitConfigRegistry = userUnitConfigRegistry;
        this.authorizationGroupConfigRegistry = authorizationGroupConfigRegistry;
    }

    @Override
    public void init(Registry<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> registry) throws InitializationException, InterruptedException {
        super.init(registry);

        try {
            UnitConfig.Builder adminGroupConfig = null;
            UnitConfig.Builder bcoGroupConfig = null;
            for (UnitConfig authorizationGroup : authorizationGroupConfigRegistry.getMessages()) {
                if (authorizationGroup.getLabel().equals(AuthorizationGroupCreationPlugin.ADMIN_GROUP_LABEL)) {
                    adminGroupConfig = authorizationGroup.toBuilder();
                } else if (authorizationGroup.getLabel().equals(AuthorizationGroupCreationPlugin.BCO_GROUP_LABEL)) {
                    bcoGroupConfig = authorizationGroup.toBuilder();
                }
            }
            if (adminGroupConfig == null) {
                throw new InitializationException(this, new NotAvailableException("Admin AuthorizationGroupUnitConfigConfig"));
            }
            if (bcoGroupConfig == null) {
                throw new InitializationException(this, new NotAvailableException("BCO AuthorizationGroupUnitConfigConfig"));
            }

            boolean adminExists = false;
            boolean bcoExists = false;
            for (UnitConfig userUnitConfig : userUnitConfigRegistry.getMessages()) {
                if (CachedAuthenticationRemote.getRemote().isAdmin(userUnitConfig.getId()).get(1, TimeUnit.SECONDS)) {
                    adminExists = true;
                    if (!adminGroupConfig.getAuthorizationGroupConfig().getMemberIdList().contains(userUnitConfig.getId())) {
                        // user is admin but not in group, so add him
                        AuthorizationGroupConfig.Builder authorizationGroup = adminGroupConfig.getAuthorizationGroupConfigBuilder();
                        authorizationGroup.addMemberId(userUnitConfig.getId());
                        authorizationGroupConfigRegistry.update(adminGroupConfig.build());
                    }
                }
                if (userUnitConfig.getUserConfig().getUserName().equals(BCO_USERNAME)) {
                    bcoExists = true;
                }
            }

            if (!adminExists) {
                registerDefaultAdmin(adminGroupConfig);
            }
            if (!bcoExists) {
                registerBCOUser(bcoGroupConfig);
            }
        } catch (CouldNotPerformException | ExecutionException | TimeoutException ex) {
            throw new InitializationException(this, new CouldNotPerformException("Could not check for register initial user accounts", ex));
        }
    }

    public void registerDefaultAdmin(UnitConfig.Builder adminGroupConfig) throws CouldNotPerformException {
        String initialRegistrationPassword = AuthenticatorController.getInitialPassword();

        if (initialRegistrationPassword == null) {
            LOGGER.error("No administator is yet registered and the initial registartion password of the authenticator is not available. Please use the bco launcher for the initial start.");
            System.exit(1);
        }

        // check if a user with the default username does not already exist in the database
        String userId = "";
        boolean defaultAdminAlreadyInRegistry = false;
        for (UnitConfig unitConfig : userUnitConfigRegistry.getMessages()) {
            if (unitConfig.getUserConfig().getUserName().equals(DEFAULT_ADMIN_USERNAME_AND_PASSWORD)) {
                defaultAdminAlreadyInRegistry = true;
                userId = unitConfig.toBuilder().getId();
            }
        }

        // if not register one
        if (!defaultAdminAlreadyInRegistry) {
            UnitConfig.Builder unitConfig = UnitConfig.newBuilder();
            unitConfig.setType(UnitType.USER);

            UserConfig.Builder userConfig = unitConfig.getUserConfigBuilder();
            userConfig.setFirstName("Initial");
            userConfig.setLastName("Admin");
            userConfig.setUserName(DEFAULT_ADMIN_USERNAME_AND_PASSWORD);

            userId = userUnitConfigRegistry.register(unitConfig.build()).getId();
        }

        // publish his credentials to the authenticator
        LoginCredentialsChange.Builder loginCredentials = LoginCredentialsChange.newBuilder();
        loginCredentials.setId(userId);
        try {
            loginCredentials.setNewCredentials(EncryptionHelper.encryptSymmetric(EncryptionHelper.hash(DEFAULT_ADMIN_USERNAME_AND_PASSWORD), EncryptionHelper.hash(initialRegistrationPassword)));
        } catch (IOException ex) {
            throw new CouldNotPerformException("Could not encrypt password", ex);
        }
        try {
            CachedAuthenticationRemote.getRemote().register(loginCredentials.build()).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not register default administrator at authenticator");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        // add him to the admin group if he is not already in it
        AuthorizationGroupConfig.Builder authorizationGroup = adminGroupConfig.getAuthorizationGroupConfigBuilder();
        if (!authorizationGroup.getMemberIdList().contains(userId)) {
            authorizationGroup.addMemberId(userId);
            authorizationGroupConfigRegistry.update(adminGroupConfig.build());
        }
    }

    public void registerBCOUser(UnitConfig.Builder bcoGroupConfig) throws CouldNotPerformException {
        try {
            String adminId = null;
            for (UnitConfig config : userUnitConfigRegistry.getMessages()) {
                if (config.getUserConfig().getUserName().equals(DEFAULT_ADMIN_USERNAME_AND_PASSWORD)) {
                    adminId = config.getId();
                }
            }
            if (adminId == null) {
                throw new NotAvailableException("adminId");
            }
            if (!SessionManager.getInstance().login(adminId, DEFAULT_ADMIN_USERNAME_AND_PASSWORD)) {
                throw new CouldNotPerformException("Login as default admin failed");
            }
        } catch (CouldNotPerformException ex) {
            LOGGER.error("Could not log in as the default admin user to create a bco user");
            throw ex;
        }

        // check if a user with the default username does not already exist in the database
        String userId = "";
        boolean bcoUserAlreadyInRegistry = false;
        for (UnitConfig unitConfig : userUnitConfigRegistry.getMessages()) {
            if (unitConfig.getUserConfig().getUserName().equals(BCO_USERNAME)) {
                bcoUserAlreadyInRegistry = true;
                userId = unitConfig.toBuilder().getId();
            }
        }

        // if not register one
        if (!bcoUserAlreadyInRegistry) {
            UnitConfig.Builder unitConfig = UnitConfig.newBuilder();
            unitConfig.setType(UnitType.USER);

            UserConfig.Builder userConfig = unitConfig.getUserConfigBuilder();
            userConfig.setFirstName("System");
            userConfig.setLastName("User");
            userConfig.setUserName(BCO_USERNAME);

            userId = userUnitConfigRegistry.register(unitConfig.build()).getId();
        }

        // register the bco user as a client at the authenticator
        SessionManager.getInstance().registerClient(userId);

        // add him to the bco group if he is not already in it
        AuthorizationGroupConfig.Builder authorizationGroup = bcoGroupConfig.getAuthorizationGroupConfigBuilder();
        if (!authorizationGroup.getMemberIdList().contains(userId)) {
            authorizationGroup.addMemberId(userId);
            authorizationGroupConfigRegistry.update(bcoGroupConfig.build());
        }
        SessionManager.getInstance().logout();
    }
}
