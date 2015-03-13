/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.rsb.ScopeType.Scope;

/**
 *
 * @author thuxohl
 */
public class ScopeContainer extends NodeContainer<Scope> {

    public ScopeContainer(Scope scope) {
        super("Scope", scope);
        super.add(scope.getStringRep(), "String Representation");
    }
}