/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.leave;

import de.citec.csra.dm.view.struct.node.Node;

/**
 *
 * @author thuxohl
 * @param <M>
 */
public interface Leave<M> extends Node {
    
    public M getValue();
            
}