package org.openbase.bco.registry.user.core;

/*
 * #%L
 * REM UserRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.openbase.bco.registry.user.core.consistency.UserConfigScopeConsistencyHandler;
import org.openbase.bco.registry.user.core.consistency.UserConfigUserNameConsistencyHandler;
import org.openbase.bco.registry.user.core.consistency.UserGroupConfigLabelConsistencyHandler;
import org.openbase.bco.registry.user.core.consistency.UserGroupConfigScopeConsistencyHandler;
import org.openbase.bco.registry.user.core.dbconvert.DummyConverter;
import org.openbase.bco.registry.user.lib.UserRegistry;
import org.openbase.bco.registry.user.lib.generator.UserConfigIdGenerator;
import org.openbase.bco.registry.user.lib.generator.UserGroupConfigIdGenerator;
import org.openbase.bco.registry.user.lib.jp.JPUserConfigDatabaseDirectory;
import org.openbase.bco.registry.user.lib.jp.JPUserGroupConfigDatabaseDirectory;
import org.openbase.bco.registry.user.lib.jp.JPUserRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBCommunicationService;
import org.openbase.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserGroupConfigType.UserGroupConfig;
import rst.authorization.UserRegistryDataType.UserRegistryData;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class UserRegistryController extends RSBCommunicationService<UserRegistryData, UserRegistryData.Builder> implements UserRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserGroupConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, UserConfig, UserConfig.Builder, UserRegistryData.Builder> userRegistry;
    private ProtoBufFileSynchronizedRegistry<String, UserGroupConfig, UserGroupConfig.Builder, UserRegistryData.Builder> userGroupRegistry;

    public UserRegistryController() throws InstantiationException, InterruptedException {
        super(UserRegistryData.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            userRegistry = new ProtoBufFileSynchronizedRegistry<>(UserConfig.class, getBuilderSetup(), getDataFieldDescriptor(UserRegistryData.USER_CONFIG_FIELD_NUMBER), new UserConfigIdGenerator(), JPService.getProperty(JPUserConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            userGroupRegistry = new ProtoBufFileSynchronizedRegistry<>(UserGroupConfig.class, getBuilderSetup(), getDataFieldDescriptor(UserRegistryData.USER_GROUP_CONFIG_FIELD_NUMBER), new UserGroupConfigIdGenerator(), JPService.getProperty(JPUserGroupConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            userRegistry.activateVersionControl(DummyConverter.class.getPackage());
            userGroupRegistry.activateVersionControl(DummyConverter.class.getPackage());

            userRegistry.loadRegistry();

            userRegistry.registerConsistencyHandler(new UserConfigUserNameConsistencyHandler());
            userRegistry.registerConsistencyHandler(new UserConfigScopeConsistencyHandler());

            userGroupRegistry.loadRegistry();

            userGroupRegistry.registerConsistencyHandler(new UserGroupConfigLabelConsistencyHandler());
            userGroupRegistry.registerConsistencyHandler(new UserGroupConfigScopeConsistencyHandler());

            userRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, UserConfig, UserConfig.Builder>>>() {

                @Override
                public void update(final Observable<Map<String, IdentifiableMessage<String, UserConfig, UserConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UserConfig, UserConfig.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

            userGroupRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder>>>() {

                @Override
                public void update(final Observable<Map<String, IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            super.init(JPService.getProperty(JPUserRegistryScope.class).getValue());
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate location registry!", ex);
        }

        try {
            userRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
        }

        try {
            userGroupRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
    }

    @Override
    public void shutdown() {
        if (userRegistry != null) {
            userRegistry.shutdown();
        }

        if (userGroupRegistry != null) {
            userGroupRegistry.shutdown();
        }

        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }

    @Override
    public final void notifyChange() throws CouldNotPerformException, InterruptedException {
        // sync read only flags
        setDataField(UserRegistryData.USER_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, userRegistry.isReadOnly());
        setDataField(UserRegistryData.GROUP_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, userGroupRegistry.isReadOnly());
        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(UserRegistry.class, this, server);
    }

    public ProtoBufFileSynchronizedRegistry<String, UserConfig, UserConfig.Builder, UserRegistryData.Builder> getUserRegistry() {
        return userRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, UserGroupConfig, UserGroupConfig.Builder, UserRegistryData.Builder> getGroupRegistry() {
        return userGroupRegistry;
    }

    @Override
    public Future<UserConfig> registerUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> userRegistry.register(userConfig));
    }

    @Override
    public Boolean containsUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return userRegistry.contains(userConfig);
    }

    @Override
    public Boolean containsUserConfigById(String userConfigId) throws CouldNotPerformException {
        return userRegistry.contains(userConfigId);
    }

    @Override
    public Future<UserConfig> updateUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> userRegistry.update(userConfig));
    }

    @Override
    public Future<UserConfig> removeUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> userRegistry.remove(userConfig));
    }

    @Override
    public UserConfig getUserConfigById(String userConfigId) throws CouldNotPerformException {
        return userRegistry.get(userConfigId).getMessage();
    }

    @Override
    public List<UserConfig> getUserConfigs() throws CouldNotPerformException {
        return userRegistry.getMessages();
    }

    @Override
    public Boolean isUserConfigRegistryReadOnly() throws CouldNotPerformException {
        return userRegistry.isReadOnly();
    }

    @Override
    public List<UserConfig> getUserConfigsByUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        List<UserConfig> userConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder> group : userGroupRegistry.getEntries()) {
            if (group.getMessage().equals(groupConfig)) {
                for (String memeberId : group.getMessage().getMemberIdList()) {
                    userConfigs.add(getUserConfigById(memeberId));
                }
                return userConfigs;
            }
        }
        return userConfigs;
    }

    @Override
    public Future<UserGroupConfig> registerUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> userGroupRegistry.register(groupConfig));
    }

    @Override
    public Boolean containsUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        return userGroupRegistry.contains(groupConfig);
    }

    @Override
    public Boolean containsUserGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return userGroupRegistry.contains(groupConfigId);
    }

    @Override
    public Future<UserGroupConfig> updateUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> userGroupRegistry.update(groupConfig));
    }

    @Override
    public Future<UserGroupConfig> removeUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> userGroupRegistry.remove(groupConfig));
    }

    @Override
    public UserGroupConfig getUserGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return userGroupRegistry.get(groupConfigId).getMessage();
    }

    @Override
    public List<UserGroupConfig> getUserGroupConfigs() throws CouldNotPerformException {
        return userGroupRegistry.getMessages();
    }

    @Override
    public List<UserGroupConfig> getUserGroupConfigsbyUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        List<UserGroupConfig> groupConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder> group : userGroupRegistry.getEntries()) {
            group.getMessage().getMemberIdList().stream().filter((memeberId) -> (userConfig.getId().equals(memeberId))).forEach((_item) -> {
                groupConfigs.add(group.getMessage());
            });
        }
        return groupConfigs;
    }

    @Override
    public Boolean isUserGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        return userGroupRegistry.isReadOnly();
    }
}
