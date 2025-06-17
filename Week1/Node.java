package Week1;
import java.util.*;

public class Node{
    private String data;
    private ArrayList<Node> children;
    private int counter;

public Node(String data,int startpoint){
    this.data = data;
    children = new ArrayList<>();
    counter = startpoint;
}

public String getData(){
    return this.data;
}

public int childLength(){
    return children.size();
}

public List<Node> getChildren(){
    return children;
}

public void addChild(Node node){
    this.children.add(node);
}

public void createChildren(int depth, int width, int target, Node root){
    int w = width;

    while(w > 0 && depth > 0){
        if(counter == target){
            Node exitnode = new Node("Exit",counter);
            root.addChild(exitnode); 
        }
        Node tempnode = new Node("Point: " + counter,counter++);
        root.addChild(tempnode);
        createChildren(depth-1, width, target, tempnode);
        w--;
    }
}
}