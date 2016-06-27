package org.openbase.bco.registry.scene.lib;

/*
 * #%L
 * REM SceneRegistry Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import org.openbase.jul.exception.CouldNotPerformException;
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.jul.iface.Shutdownable;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;

/**
 *
 * @author mpohling
 */
public interface SceneRegistry extends Shutdownable {

    public Future<SceneConfig> registerSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException;

    public Boolean containsSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException;

    public Boolean containsSceneConfigById(String sceneConfigId) throws CouldNotPerformException;

    public Future<SceneConfig> updateSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException;

    public Future<SceneConfig> removeSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException;

    public SceneConfig getSceneConfigById(final String sceneConfigId) throws CouldNotPerformException;

    public List<SceneConfig> getSceneConfigs() throws CouldNotPerformException;

    public Boolean isSceneConfigRegistryReadOnly() throws CouldNotPerformException;
}
