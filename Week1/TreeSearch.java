package Week1;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

public class TreeSearch {

    private static Node dfs(Node root) {
        if (root.getData().equals("Exit"))
            return root;
        else {
            for (int i = 0; i < root.childLength(); i++) {
                System.out.println(root.getChildren().get(i).getData());
                Node child = dfs(root.getChildren().get(i)); // <--- recursion
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }

    public static Node dfsiterative(Node node, String target) {
        Stack<Node> stack = new Stack<>();
        stack.push(node);

        while (!stack.isEmpty()) {
            Node current = stack.pop();
            if (current.getData().equals(target))
                return current;
            List<Node> children = current.getChildren();
            System.out.println(current.getData());
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i)); // Right to left
            }
        }
        return null;
    }

    private static Node bfsqueue(Node root) {
        if (root.getData().equals("Exit"))
            return root;
        else {
            Queue<Node> queue = new LinkedList<>();
            queue.addAll(root.getChildren());
            Node child = null;

            while (!queue.isEmpty()) {
                child = queue.poll();
                System.out.println(child.getData());
                if (child.getData().equals("Exit"))
                    return child;
                queue.addAll(child.getChildren());
            }

        }
        return null;
    }

    private static Node dfsstack(Node root) {
        if (root.getData().equals("Exit"))
            return root;
        else {
            Stack<Node> stack = new Stack<>();
            for (int i = root.childLength() - 1; i >= 0; i--) {
                stack.add(root.getChildren().get(i));
            }
            Node child = null;

            while (!stack.isEmpty()) {
                child = stack.pop();
                System.out.println(child.getData());
                if (child.getData().equals("Exit"))
                    return child;
                for (int i = child.childLength() - 1; i >= 0; i--) {
                    stack.add(child.getChildren().get(i));
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {

        Node start = new Node("Start", 1);
        start.createChildren(3, 2, 16234, start);

        long startTime = System.currentTimeMillis();
        bfsqueue(start);
        long finishTime = System.currentTimeMillis();
        System.out.println("finish Time: " + (finishTime - startTime));

        startTime = System.currentTimeMillis();
        dfsstack(start);
        finishTime = System.currentTimeMillis();
        System.out.println("finish Time: " + (finishTime - startTime));

        startTime = System.currentTimeMillis();
        dfsiterative(start, "Exit");
        finishTime = System.currentTimeMillis();
        System.out.println("finish Time: " + (finishTime - startTime));

        startTime = System.currentTimeMillis();
        dfs(start);
        finishTime = System.currentTimeMillis();
        System.out.println("finish Time: " + (finishTime - startTime));
    }
}