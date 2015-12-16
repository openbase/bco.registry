/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.scm.core;

import de.citec.jp.JPSceneClassDatabaseDirectory;
import de.citec.jp.JPSceneConfigDatabaseDirectory;
import de.citec.jp.JPSceneRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPDebugMode;
import de.citec.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.MultiException;
import de.citec.jul.exception.VerificationFailedException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.storage.registry.jp.JPGitRegistryPlugin;
import de.citec.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import de.citec.jul.storage.registry.jp.JPInitializeDB;
import de.citec.scm.core.registry.SceneRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class SceneManager {

    private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);

    public static final String APP_NAME = SceneManager.class.getSimpleName();

    private final SceneRegistryService sceneRegistry;

    public SceneManager() throws InitializationException, InterruptedException {
        try {
            this.sceneRegistry = new SceneRegistryService();
            this.sceneRegistry.init();
            this.sceneRegistry.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        if (sceneRegistry != null) {
            sceneRegistry.shutdown();
        }
    }

    public SceneRegistryService getSceneRegistry() {
        return sceneRegistry;
    }


    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPSceneRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPSceneConfigDatabaseDirectory.class);
        JPService.registerProperty(JPSceneClassDatabaseDirectory.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.parseAndExitOnError(args);

        SceneManager sceneManager;
        try {
            sceneManager = new SceneManager();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }

        MultiException.ExceptionStack exceptionStack = null;

        if (!sceneManager.getSceneRegistry().getSceneConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(sceneManager, new VerificationFailedException("SceneConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        try {
            MultiException.checkAndThrow(APP_NAME + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
