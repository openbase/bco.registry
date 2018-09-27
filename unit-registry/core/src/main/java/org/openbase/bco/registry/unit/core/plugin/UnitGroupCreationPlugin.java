package org.openbase.bco.registry.unit.core.plugin;

/*-
 * #%L
 * BCO Registry Unit Core
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

import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupCreationPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitGroupCreationPlugin.class);

    private final UnitRegistry unitRegistry;

    public UnitGroupCreationPlugin(final UnitRegistry unitRegistry) {
        this.unitRegistry = unitRegistry;
    }

    @Override
    public void afterRegister(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        final Set<UnitType> unitTypesWithOperationService = new HashSet<>();
        for (final UnitTemplate unitTemplate : CachedTemplateRegistryRemote.getRegistry().getUnitTemplates()) {
            for (ServiceDescription serviceDescription : unitTemplate.getServiceDescriptionList()) {
                if (serviceDescription.getPattern() == ServicePattern.OPERATION) {
                    unitTypesWithOperationService.add(unitTemplate.getType());
                    break;
                }
            }
        }
        for (final UnitConfig unitConfig : unitRegistry.getUnitConfigs(UnitType.LOCATION)) {
            LOGGER.info("Test location {}", ScopeGenerator.generateStringRep(unitConfig.getScope()));
            final Map<String, Map<UnitType, List<String>>> unitLabelTypeIdMapping = new HashMap<>();
            for (final UnitConfig config : unitRegistry.getUnitConfigsByLocation(unitConfig.getId(), false)) {
                if (config.getUnitType() == UnitType.UNIT_GROUP || !unitTypesWithOperationService.contains(config.getUnitType())) {
                    continue;
                }

                // unitType, matches pattern -> label, ids
                final String label = LabelProcessor.getBestMatch(config.getLabel());
//                final Matcher matcher = compile.matcher(label);
//                if (!matcher.find()) {
//                    continue;
//                }


                String[] s = label.split(" ");
                if (s.length == 1) {
                    continue;
                }
                try {
                    Integer.parseInt(s[s.length - 1]);
                } catch (NumberFormatException ex) {
                    continue;
                }
                LOGGER.info("Found match in label {}", label);
                String a = "";
                for (int i = 0; i < s.length - 1; i++) {
                    a += s[i];
                    if (i < (s.length - 2)) {
                        a += " ";
                    }
                }
                LOGGER.info("Extract label [{}]", a);

                if (!unitLabelTypeIdMapping.containsKey(a)) {
                    unitLabelTypeIdMapping.put(a, new HashMap<>());
                }

                Map<UnitType, List<String>> unitTypeListMap = unitLabelTypeIdMapping.get(a);
                if (!unitTypeListMap.containsKey(config.getUnitType())) {
                    unitTypeListMap.put(config.getUnitType(), new ArrayList<>());
                }

                unitTypeListMap.get(config.getUnitType()).add(config.getId());
            }

            for (final Entry<String, Map<UnitType, List<String>>> entryTop : unitLabelTypeIdMapping.entrySet()) {
                for (Entry<UnitType, List<String>> entry : entryTop.getValue().entrySet()) {
                    if (entry.getValue().size() < 2) {
                        continue;
                    }

                    final Builder builder = UnitConfig.newBuilder().setUnitType(UnitType.UNIT_GROUP);
                    LabelProcessor.addLabel(builder.getLabelBuilder(), Locale.getDefault(), entryTop.getKey());
                    UnitGroupConfig.Builder unitGroupConfigBuilder = builder.getUnitGroupConfigBuilder();
                    unitGroupConfigBuilder.setUnitType(entry.getKey());
                    unitGroupConfigBuilder.addAllMemberId(entry.getValue());

                    LOGGER.info("Would create group:\n{}", builder.build());
                }
            }
        }
    }
}
