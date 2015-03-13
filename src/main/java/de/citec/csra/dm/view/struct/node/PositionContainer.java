/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.math.Vec3DFloatType.Vec3DFloat;

/**
 *
 * @author thuxohl
 */
public class PositionContainer extends NodeContainer<Vec3DFloat> {

    public PositionContainer(Vec3DFloat position) {
        super("Position", position);
        super.add(position.getX(), "X Position");
        super.add(position.getY(), "Y Position");
        super.add(position.getZ(), "Z Position");
    }
}