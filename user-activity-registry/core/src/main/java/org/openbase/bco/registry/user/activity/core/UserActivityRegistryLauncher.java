package org.openbase.bco.registry.user.activity.core;

/*
 * #%L
 * BCO Registry User Activity Core
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.bco.registry.lib.BCO;
import org.openbase.bco.registry.lib.launch.AbstractRegistryLauncher;
import org.openbase.jul.pattern.launch.AbstractLauncher;
import org.openbase.bco.registry.user.activity.lib.UserActivityRegistry;
import org.openbase.bco.registry.user.activity.lib.jp.JPUserActivityClassDatabaseDirectory;
import org.openbase.bco.registry.user.activity.lib.jp.JPUserActivityConfigDatabaseDirectory;
import org.openbase.bco.registry.user.activity.lib.jp.JPUserActivityRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPForce;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.extension.rsb.com.jp.JPRSBHost;
import org.openbase.jul.extension.rsb.com.jp.JPRSBPort;
import org.openbase.jul.extension.rsb.com.jp.JPRSBTransport;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserActivityRegistryLauncher extends AbstractRegistryLauncher<UserActivityRegistryController> {

    public UserActivityRegistryLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(UserActivityRegistry.class, UserActivityRegistryController.class);
    }

    @Override
    public void loadProperties() {
        JPService.registerProperty(JPUserActivityRegistryScope.class);
        JPService.registerProperty(JPUserActivityClassDatabaseDirectory.class);
        JPService.registerProperty(JPUserActivityConfigDatabaseDirectory.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);

        JPService.registerProperty(JPRSBHost.class);
        JPService.registerProperty(JPRSBPort.class);
        JPService.registerProperty(JPRSBTransport.class);
    }

    public static void main(String args[]) throws Throwable {
        BCO.printLogo();
        AbstractLauncher.main(args, UserActivityRegistry.class, UserActivityRegistryLauncher.class);
    }
}