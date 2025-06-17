package Week1;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;


public class LVTree {


    // Depth-First Search (recursive)
    public static Node DFS(Node node, String target) {
        if (node.getData().equals(target))
            return node;
        for (Node child : node.getChildren()) {
            Node result = DFS(child, target);
            if (result != null)
                return result;
        }
        return null;
    }

    // Breadth-First Search
    public static Node BFS(Node node, String target) {
        Queue<Node> queue = new LinkedList<>();
        queue.add(node);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current.getData().equals(target))
                return current;
            queue.addAll(current.getChildren());
        }
        return null;
    }

    public static void main(String[] args) {
        Node start = new Node("Start", 2);
        start.createChildren(3, 2, 16234, start);
        
    }
}
