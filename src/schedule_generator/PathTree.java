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

package schedule_generator;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * [Class]: PathTree
 * [Usage]: Used to specify the path on publish subscribe
 * flows. It is basically a tree of path nodes with a few
 * simple and classic tree methods.
 * 
 */
public class PathTree implements Serializable {

	private static final long serialVersionUID = 1L;
	private PathNode root;
    private ArrayList<PathNode> leaves;

    /**
     * [Method]: addRoot
     * [Usage]: Adds a root node to the pathTree.
     * The user must give a device or switch to 
     * be the root of the tree.
     * 
     * @param node      Device of the root node of the pathTree
     * @return          A reference to the root
     */
    public PathNode addRoot(Object node)
    {
        root=new PathNode(node);
        root.setParent(null);
        root.setChildren(new ArrayList<PathNode>());
        return root;
    }


    /**
     * [Method]: changeRoot
     * [Usage]: Given a new PathNode object, make it the
     * new root of this pathTree. Old root becomes child
     * of new root.
     * 
     * @param newRoot       New root of pathTree
     */
    public void changeRoot(PathNode newRoot)
    {
        PathNode oldRoot=this.root;
        newRoot.setParent(null);
        newRoot.addChild(oldRoot);
        oldRoot.setParent(newRoot);
        this.root=newRoot;
    }

    
    /**
     * [Method]: searchLeaves
     * [Usage]: Adds all leaves to the leaves ArrayList starting
     * from the node given as a parameter. In the way it is 
     * implemented, must be used only once.
     * 
     * TODO [Priority: Low]: Renew list on every first call
     * 
     * @param node      Starter node of the search
     */
    public void searchLeaves(PathNode node) {
        
        if(node.getChildren().size() == 0) {
            leaves.add(node);
            return;
        }
        
        for(PathNode auxNode : node.getChildren()) {
            searchLeaves(auxNode);
        }
        
    }
    
    
    /**
     * [Method]: getLeaves
     * [Usage]: Returns an ArrayList with all the nodes of the 
     * pathTree.
     * 
     * @return      ArrayList with all leaves as PathNodes
     */
    public ArrayList<PathNode> getLeaves(){
        leaves = new ArrayList<PathNode>();
        
        searchLeaves(root);
        
        return leaves;
    }
    
    public PathNode searchNode(String name, PathNode searchPoint) {
    	PathNode ret = null;
    	
    	if(
			name.equals( 
    			(searchPoint.getNode() instanceof Device? ((Device)searchPoint.getNode()).getName(): ((TSNSwitch)searchPoint.getNode()).getName())
			)
		) {
    		return searchPoint;
    	}
		
    	for(PathNode child : searchPoint.getChildren()) {
    		ret = this.searchNode(name, child);
    		if(ret != null) {
    			break;
    		}
    	}
    	
    	return ret;
    }
    
    /*
     * GETTERS AND SETTERS
     */
    
    
    public PathNode getRoot() {
        return root;
    }

    public void setRoot(PathNode root) {
        this.root = root;
    }

}
