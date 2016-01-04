/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.lib.generator;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdGenerator;
import java.util.UUID;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class LocationIDGenerator implements IdGenerator<String, LocationConfig> {

    @Override
    public String generateId(LocationConfig message) throws CouldNotPerformException {
        return UUID.randomUUID().toString();
    }
}