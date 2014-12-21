package net.clonecomputers.lab.starburst.properties;

import java.util.*;

public interface PropertyTree extends PropertyTreeNode {
	public Map<String, PropertyTreeNode> subproperties();
}
