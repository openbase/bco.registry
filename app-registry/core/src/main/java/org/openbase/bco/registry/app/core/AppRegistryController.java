package org.openbase.bco.registry.app.core;

/*
 * #%L
 * REM AppRegistry Core
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
import java.util.concurrent.Future;
import org.openbase.bco.registry.app.core.dbconvert.DummyConverter;
import org.openbase.bco.registry.app.lib.AppRegistry;
import org.openbase.bco.registry.app.lib.generator.AppClassIdGenerator;
import org.openbase.bco.registry.app.lib.jp.JPAppClassDatabaseDirectory;
import org.openbase.bco.registry.app.lib.jp.JPAppRegistryScope;
import org.openbase.bco.registry.lib.controller.AbstractRegistryController;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.RemoteRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.app.AppClassType.AppClass;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.app.AppRegistryDataType.AppRegistryData;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class AppRegistryController extends AbstractRegistryController<AppRegistryData, AppRegistryData.Builder> implements AppRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppClass.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, AppClass, AppClass.Builder, AppRegistryData.Builder> appClassRegistry;

    private final RemoteRegistry<String, UnitConfig, UnitConfig.Builder, AppRegistryData.Builder> appUnitConfigRemoteRegistry;
    private final UnitRegistryRemote unitRegistryRemote;

    public AppRegistryController() throws InstantiationException, InterruptedException {
        super(JPAppRegistryScope.class, AppRegistryData.newBuilder());
        try {
            appClassRegistry = new ProtoBufFileSynchronizedRegistry<>(AppClass.class, getBuilderSetup(), getDataFieldDescriptor(AppRegistryData.APP_CLASS_FIELD_NUMBER), new AppClassIdGenerator(), JPService.getProperty(JPAppClassDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            appUnitConfigRemoteRegistry = new RemoteRegistry<>();
            unitRegistryRemote = new UnitRegistryRemote();
        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        super.init();
        unitRegistryRemote.addDataObserver(new Observer<UnitRegistryData>() {

            @Override
            public void update(Observable<UnitRegistryData> source, UnitRegistryData data) throws Exception {
                appUnitConfigRemoteRegistry.notifyRegistryUpdate(data.getAppUnitConfigList());
                setDataField(AppRegistryData.APP_UNIT_CONFIG_FIELD_NUMBER, data.getAppUnitConfigList());
                setDataField(AppRegistryData.APP_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, data.getAppUnitConfigRegistryConsistent());
                setDataField(AppRegistryData.APP_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, data.getAppUnitConfigRegistryReadOnly());
                notifyChange();
            }
        });
    }

    @Override
    public void shutdown() {
        super.shutdown();
        appUnitConfigRemoteRegistry.shutdown();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void activateVersionControl() throws CouldNotPerformException {
        appClassRegistry.activateVersionControl(DummyConverter.class.getPackage());
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void loadRegistries() throws CouldNotPerformException {
        appClassRegistry.loadRegistry();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistryRemotes() throws CouldNotPerformException {
        registerRegistryRemote(unitRegistryRemote);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerDependencies() throws CouldNotPerformException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void removeDependencies() throws CouldNotPerformException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerPlugins() throws CouldNotPerformException, InterruptedException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistries() throws CouldNotPerformException {
        registerRegistry(appClassRegistry);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void performInitialConsistencyCheck() throws CouldNotPerformException, InterruptedException {
        try {
            appClassRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
    }

    @Override
    public final void syncDataTypeFlags() throws CouldNotPerformException, InterruptedException {
        setDataField(AppRegistryData.APP_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, appClassRegistry.isReadOnly());
        setDataField(AppRegistryData.APP_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, appClassRegistry.isConsistent());
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(AppRegistry.class, this, server);
    }

    @Override
    public Future<UnitConfig> registerAppConfig(UnitConfig appUnitConfig) throws CouldNotPerformException {
        return unitRegistryRemote.registerUnitConfig(appUnitConfig);
    }

    @Override
    public UnitConfig getAppConfigById(String appUnitConfigId) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return appUnitConfigRemoteRegistry.get(appUnitConfigId).getMessage();
    }

    @Override
    public Boolean containsAppConfigById(String appUnitConfigId) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return appUnitConfigRemoteRegistry.contains(appUnitConfigId);
    }

    @Override
    public Boolean containsAppConfig(UnitConfig appUnitConfig) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return appUnitConfigRemoteRegistry.contains(appUnitConfig);
    }

    @Override
    public Future<UnitConfig> updateAppConfig(UnitConfig appUnitConfig) throws CouldNotPerformException {
        return unitRegistryRemote.updateUnitConfig(appUnitConfig);
    }

    @Override
    public Future<UnitConfig> removeAppConfig(UnitConfig appUnitConfig) throws CouldNotPerformException {
        return unitRegistryRemote.removeUnitConfig(appUnitConfig);
    }

    @Override
    public List<UnitConfig> getAppConfigs() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return appUnitConfigRemoteRegistry.getMessages();
    }

    @Override
    public Boolean isAppConfigRegistryReadOnly() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return getData().getAppUnitConfigRegistryReadOnly();
    }

    @Override
    public List<UnitConfig> getAppConfigsByAppClass(AppClass appClass) throws CouldNotPerformException, InterruptedException {
        return getAppConfigsByAppClassId(appClass.getId());
    }

    @Override
    public List<UnitConfig> getAppConfigsByAppClassId(String appClassId) throws CouldNotPerformException, InterruptedException {
        if (!containsAppClassById(appClassId)) {
            throw new NotAvailableException("appClassId [" + appClassId + "]");
        }

        unitRegistryRemote.validateData();
        List<UnitConfig> appConfigs = new ArrayList<>();
        for (UnitConfig appConfig : getAppConfigs()) {
            if (appConfig.getAppConfig().getAppClassId().equals(appClassId)) {
                appConfigs.add(appConfig);
            }
        }
        return appConfigs;
    }

    @Override
    public Future<AppClass> registerAppClass(AppClass appClass) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> appClassRegistry.register(appClass));
    }

    @Override
    public Boolean containsAppClass(AppClass appClass) throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.contains(appClass);
    }

    @Override
    public Boolean containsAppClassById(String appClassId) throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.contains(appClassId);
    }

    @Override
    public Future<AppClass> updateAppClass(AppClass appClass) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> appClassRegistry.update(appClass));
    }

    @Override
    public Future<AppClass> removeAppClass(AppClass appClass) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> appClassRegistry.remove(appClass));
    }

    @Override
    public AppClass getAppClassById(String appClassId) throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.getMessage(appClassId);
    }

    @Override
    public List<AppClass> getAppClasses() throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.getMessages();
    }

    @Override
    public Boolean isAppClassRegistryReadOnly() throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isAppClassRegistryConsistent() throws CouldNotPerformException {
        return appClassRegistry.isConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isAppConfigRegistryConsistent() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return getData().getAppUnitConfigRegistryConsistent();
    }

    public ProtoBufFileSynchronizedRegistry<String, AppClass, AppClass.Builder, AppRegistryData.Builder> getAppClassRegistry() {
        return appClassRegistry;
    }
}
