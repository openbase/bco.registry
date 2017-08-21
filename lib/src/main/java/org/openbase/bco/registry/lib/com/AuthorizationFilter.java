package org.openbase.bco.registry.lib.com;

/*-
 * #%L
 * BCO Registry Lib
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
import org.openbase.bco.authentication.lib.AuthorizationHelper;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthorizationFilter extends AbstractFilter<UnitConfig> {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthorizationFilter.class);
    
    private SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> authorizationGroupRegistry;
    
    public AuthorizationFilter() {
    }
    
    public void setAuthorizationGroupRegistry(final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> authorizationGroupRegistry) {
        this.authorizationGroupRegistry = authorizationGroupRegistry;
    }
    
    @Override
    public void beforeFilter() throws CouldNotPerformException {
        try {
            CachedAuthenticationRemote.getRemote();
            SessionManager.getInstance().isAuthenticated();
        } catch (CouldNotPerformException ex) {
            if (ex.getCause() instanceof InvalidStateException) {
                System.out.println("Could not check authenticated because in shutdown");
            } else {
                throw new CouldNotPerformException("Authentication failed", ex);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public boolean verify(UnitConfig unitConfig) {
        if (authorizationGroupRegistry != null) {
            return AuthorizationHelper.canAccess(unitConfig.getPermissionConfig(), SessionManager.getInstance().getUserAtClientId(), authorizationGroupRegistry.getEntryMap());
        } else {
            return AuthorizationHelper.canAccess(unitConfig.getPermissionConfig(), SessionManager.getInstance().getUserAtClientId(), null);
        }
    }
}
