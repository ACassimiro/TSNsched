//TSNsched uses the Z3 theorem solver to generate traffic schedules for Time Sensitive Networking (TSN)
//
//    Copyright (C) 2021  Aellison Cassimiro
//    
//    TSNsched is licensed under the GNU GPL version 3 or later:
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//    
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <https://www.gnu.org/licenses/>.

package network;

import java.io.Serializable;
import java.util.ArrayList;

import nodes.Device;
import nodes.Switch;
import nodes.TSNSwitch;


/**
 * [Class]: PathNode
 * [Usage]: Contains the data needed in each node of 
 * a pathTree. Can reference a father, possesses an 
 * device or switch, a list of children and a flow 
 * fragment for each children in case of being a switch.
 * 
 */
public class PathNode implements Serializable {

	private static final long serialVersionUID = 1L;
	private PathNode parent; // The parent of the current FlowNode
    private Object node;
    private ArrayList<PathNode> children; // The children of the current FlowNode
    private ArrayList<FlowFragment> flowFragments;
   
    
    /**
     * [Method]: PathNode
     * [Usage]: Overloaded constructor method of the this class.
     * Receives an object that must be either a device or a switch.
     * In case of switch, creates a list of children and flowFragments.
     * 
     * @param node      Device or a Switch that represents the node
     */
    public PathNode (Object node)
    {
        if((node instanceof TSNSwitch) || (node instanceof Switch)) {
            this.node = node;
            children = new ArrayList<PathNode>();
            flowFragments = new ArrayList<FlowFragment>();
        } else if (node instanceof Device) {
            this.node = node;
            children = null;
        } else {
            //[TODO]: Throw error
        }
        
        children  = new ArrayList<PathNode>();
    }

    /**
     * [Method]: addChild
     * [Usage]: Adds a child to this node.
     * 
     * @param node      Object representing the child device or switch
     * @return          A reference to the newly created node
     */
    public PathNode addChild(Object node)
    {
        PathNode pathNode = new PathNode(node);
        pathNode.setParent(this);
        children.add(pathNode);
                
        return pathNode;
    }
    
    /*
     * GETTERS AND SETTERS:
     */
    
    public PathNode getParent() {
        return parent;
    }

    public void setParent(PathNode parent) {
        this.parent = parent;
    }

    public Object getNode() {
        return node;
    }

    public void setNode(Object node) {
        this.node = node;
    }

    public ArrayList<PathNode> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<PathNode> children) {
        this.children = children;
    }

    public void addFlowFragment(FlowFragment flowFragment) {
        this.flowFragments.add(flowFragment);
    }
    
    public ArrayList<FlowFragment> getFlowFragments() {
        return flowFragments;
    }

    public void setFlowFragment(ArrayList<FlowFragment>  flowFragments) {
        this.flowFragments = flowFragments;
    }
    
}
