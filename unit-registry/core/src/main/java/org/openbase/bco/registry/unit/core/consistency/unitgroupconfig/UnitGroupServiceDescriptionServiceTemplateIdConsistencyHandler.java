package org.openbase.bco.registry.unit.core.consistency.unitgroupconfig;

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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupServiceDescriptionServiceTemplateIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, ServiceTemplate, ServiceTemplate.Builder, UnitRegistryData.Builder> serviceTemplateRegistry;

    public UnitGroupServiceDescriptionServiceTemplateIdConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, ServiceTemplate, ServiceTemplate.Builder, UnitRegistryData.Builder> serviceTemplateRegistry) {
        this.serviceTemplateRegistry = serviceTemplateRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();
        UnitGroupConfig.Builder unitGroup = unitConfig.getUnitGroupConfigBuilder();

        boolean modification = false;
        for (ServiceDescription.Builder serviceDescription : unitGroup.getServiceDescriptionBuilderList()) {
            if (serviceDescription.getServiceTemplateId().isEmpty()) {
                if (!serviceDescription.hasType()) {
                    throw new NotAvailableException("ServiceType");
                }

                serviceDescription.setServiceTemplateId(getServiceIdByType(serviceDescription.getType()));
                modification = true;
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(unitConfig), this);
        }
    }

    public String getServiceIdByType(ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException {
        for (ServiceTemplateType.ServiceTemplate serviceTemplate : serviceTemplateRegistry.getMessages()) {
            if (serviceTemplate.getType() == serviceType) {
                return serviceTemplate.getId();
            }
        }
        throw new NotAvailableException("Now service template for type[" + serviceType.name() + "] found!", "service id");
    }
    
}