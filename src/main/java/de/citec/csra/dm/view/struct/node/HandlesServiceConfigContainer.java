/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.service.HandlesServiceConfigType.HandlesServiceConfig;

/**
 *
 * @author thuxohl
 */
class HandlesServiceConfigContainer extends NodeContainer<HandlesServiceConfig> {
    
    public HandlesServiceConfigContainer(HandlesServiceConfig handlesServiceConfig) {
        super("Handles Service Configuration", handlesServiceConfig);
        super.add(handlesServiceConfig.getHardwareConfig(), "Hardware Configuration");
    }
    
}