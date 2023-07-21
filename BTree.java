import java.util.*;

public class BTree {
    private BTreeNode root;
    private int t;

    public BTree(int t) {
        this.root = null;
        this.t = t;
    }

    // B-tree node
    private static class BTreeNode {
        private int[] keys;
        private int n;
        private BTreeNode[] children;
        private boolean leaf;

        private BTreeNode(int t, boolean leaf) {
            this.keys = new int[2 * t - 1];
            this.children = new BTreeNode[2 * t];
            this.n = 0;
            this.leaf = leaf;
        }
    }

    public void insert(int key) {
        if (root == null) {
            root = new BTreeNode(t, true);
            root.keys[0] = key;
            root.n = 1;
        } else {
            if (root.n == (2 * t - 1)) {
                BTreeNode newRoot = new BTreeNode(t, false);
                newRoot.children[0] = root;
                splitChild(newRoot, 0, root);
                insertNonFull(newRoot, key);
                root = newRoot;
            } else {
                insertNonFull(root, key);
            }
        }
    }

    private void insertNonFull(BTreeNode node, int key) {
    int i = node.n - 1;
    if (node.leaf) {
        while (i >= 0 && key < node.keys[i]) {
            node.keys[i + 1] = node.keys[i];
            i--;
        }
        // Check for duplicates
        if (i >= 0 && key == node.keys[i]) {
            System.out.println("Duplicate key. Insertion failed.");
            return;
        }
        node.keys[i + 1] = key;
        node.n++;
    } else {
        while (i >= 0 && key < node.keys[i]) {
            i--;
        }
        i++;
        if (node.children[i].n == (2 * t - 1)) {
            splitChild(node, i, node.children[i]);
            if (key > node.keys[i]) {
                i++;
            }
        }
        insertNonFull(node.children[i], key);
    }
}


    private void splitChild(BTreeNode parentNode, int index, BTreeNode childNode) {
        BTreeNode newNode = new BTreeNode(t, childNode.leaf);
        newNode.n = t - 1;

        for (int j = 0; j < (t - 1); j++) {
            newNode.keys[j] = childNode.keys[j + t];
        }

        if (!childNode.leaf) {
            for (int j = 0; j < t; j++) {
                newNode.children[j] = childNode.children[j + t];
            }
        }

        for (int j = parentNode.n; j > index; j--) {
            parentNode.children[j + 1] = parentNode.children[j];
        }

        parentNode.children[index + 1] = newNode;

        for (int j = parentNode.n - 1; j >= index; j--) {
            parentNode.keys[j + 1] = parentNode.keys[j];
        }

        parentNode.keys[index] = childNode.keys[t - 1];
        parentNode.n++;
        childNode.n = t - 1;
    }

    public boolean delete(int key) {
    if (root == null) {
        System.out.println("The tree is empty.");
        return false;
    }

    boolean keyExists = search(key);
    if (keyExists) {
        deleteKey(root, key);

        if (root.n == 0) {
            if (root.leaf) {
                root = null;
            } else {
                root = root.children[0];
            }
        }

        return true;
    } else {
        System.out.println("Key not found in the tree.");
        return false;
    }
}



    private void deleteKey(BTreeNode node, int key) {
        int i = 0;
        while (i < node.n && key > node.keys[i]) {
            i++;
        }

        if (node.leaf) {
            if (i < node.n && key == node.keys[i]) {
                removeFromLeaf(node, i);
            } else {
                System.out.println("Key " + key + " not found in the tree.");
            }
        } else {
            if (i < node.n && key == node.keys[i]) {
                removeFromInternalNode(node, i);
            } else {
                boolean isLastChild = (i == node.n);
                BTreeNode childNode = node.children[i];
                if (childNode.n < t) {
                    fillChild(node, i);
                }
                if (isLastChild && i > node.n) {
                    deleteKey(node.children[i - 1], key);
                } else {
                    deleteKey(node.children[i], key);
                }
            }
        }
    }

    private void removeFromLeaf(BTreeNode node, int index) {
        for (int i = index + 1; i < node.n; i++) {
            node.keys[i - 1] = node.keys[i];
        }
        node.n--;
    }

    private void removeFromInternalNode(BTreeNode node, int index) {
        int key = node.keys[index];
        BTreeNode predecessor = node.children[index];
        if (predecessor.n >= t) {
            int predKey = getPredecessor(predecessor);
            node.keys[index] = predKey;
            deleteKey(predecessor, predKey);
        } else {
            BTreeNode successor = node.children[index + 1];
            if (successor.n >= t) {
                int succKey = getSuccessor(successor);
                node.keys[index] = succKey;
                deleteKey(successor, succKey);
            } else {
                mergeChildren(node, index);
                deleteKey(predecessor, key);
            }
        }
    }

    private int getPredecessor(BTreeNode node) {
        while (!node.leaf) {
            node = node.children[node.n];
        }
        return node.keys[node.n - 1];
    }

    private int getSuccessor(BTreeNode node) {
        while (!node.leaf) {
            node = node.children[0];
        }
        return node.keys[0];
    }

    private void fillChild(BTreeNode node, int index) {
        BTreeNode child = node.children[index];
        BTreeNode leftSibling = (index > 0) ? node.children[index - 1] : null;
        BTreeNode rightSibling = (index < node.n) ? node.children[index + 1] : null;

        if (leftSibling != null && leftSibling.n >= t) {
            borrowFromLeftSibling(node, index);
        } else if (rightSibling != null && rightSibling.n >= t) {
            borrowFromRightSibling(node, index);
        } else if (leftSibling != null) {
            mergeChildren(node, index - 1);
        } else {
            mergeChildren(node, index);
        }
    }

    private void borrowFromLeftSibling(BTreeNode node, int index) {
        BTreeNode child = node.children[index];
        BTreeNode leftSibling = node.children[index - 1];

        // Shift keys and children of child to the right
        for (int i = child.n - 1; i >= 0; i--) {
            child.keys[i + 1] = child.keys[i];
        }

        if (!child.leaf) {
            for (int i = child.n; i >= 0; i--) {
                child.children[i + 1] = child.children[i];
            }
        }

        // Move key from the parent to child
        child.keys[0] = node.keys[index - 1];

        // Move the rightmost key from left sibling to parent
        node.keys[index - 1] = leftSibling.keys[leftSibling.n - 1];

        // Move the rightmost child from left sibling to child
        if (!child.leaf) {
            child.children[0] = leftSibling.children[leftSibling.n];
        }

        child.n++;
        leftSibling.n--;
    }

    private void borrowFromRightSibling(BTreeNode node, int index) {
        BTreeNode child = node.children[index];
        BTreeNode rightSibling = node.children[index + 1];

        // Move key from the parent to child
        child.keys[child.n] = node.keys[index];

        // Move the leftmost key from right sibling to parent
        node.keys[index] = rightSibling.keys[0];

        // Shift keys and children of right sibling to the left
        for (int i = 1; i < rightSibling.n; i++) {
            rightSibling.keys[i - 1] = rightSibling.keys[i];
        }

        if (!rightSibling.leaf) {
            for (int i = 1; i <= rightSibling.n; i++) {
                rightSibling.children[i - 1] = rightSibling.children[i];
            }
        }

        child.n++;
        rightSibling.n--;
    }

    private void mergeChildren(BTreeNode node, int index) {
        BTreeNode leftChild = node.children[index];
        BTreeNode rightChild = node.children[index + 1];

        leftChild.keys[leftChild.n] = node.keys[index];

        for (int i = 0; i < rightChild.n; i++) {
            leftChild.keys[leftChild.n + 1 + i] = rightChild.keys[i];
        }

        if (!leftChild.leaf) {
            for (int i = 0; i <= rightChild.n; i++) {
                leftChild.children[leftChild.n + 1 + i] = rightChild.children[i];
            }
        }

        for (int i = index + 1; i < node.n; i++) {
            node.keys[i - 1] = node.keys[i];
        }

        for (int i = index + 2; i <= node.n; i++) {
            node.children[i - 1] = node.children[i];
        }

        leftChild.n += rightChild.n + 1;
        node.n--;
    }

    public boolean search(int key) {
        return searchKey(root, key);
    }

    private boolean searchKey(BTreeNode node, int key) {
        int i = 0;
        while (i < node.n && key > node.keys[i]) {
            i++;
        }

        if (i < node.n && key == node.keys[i]) {
            return true;
        } else if (node.leaf) {
            return false;
        } else {
            return searchKey(node.children[i], key);
        }
    }

    public void print() {
        if (root == null) {
            System.out.println("The tree is empty.");
            return;
        }

        printTree(root, "");
    }

    private void printTree(BTreeNode node, String indent) {
        System.out.print(indent);
        for (int i = 0; i < node.n; i++) {
            System.out.print(node.keys[i] + " ");
        }
        System.out.println();

        if (!node.leaf) {
            for (int i = 0; i <= node.n; i++) {
                printTree(node.children[i], indent + "  ");
            }
        }
    }

    public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    BTree bTree = new BTree(3);

    int choice;
    do {
        System.out.println("B-tree Operations:");
        System.out.println("1. Insert");
        System.out.println("2. Delete");
        System.out.println("3. Search");
        System.out.println("4. Display");
        System.out.println("5. Quit");
        System.out.print("Enter your choice: ");
        choice = scanner.nextInt();

        switch (choice) {
            case 1:
                System.out.print("Enter the key to insert: ");
                int insertKey = scanner.nextInt();
                bTree.insert(insertKey);
                System.out.println("Key inserted successfully.");
                break;
            case 2:
                System.out.print("Enter the key to delete: ");
                int deleteKey = scanner.nextInt();
                if (bTree.delete(deleteKey)) {
                    System.out.println("Key deleted successfully.");
                } else {
                    System.out.println("Key not found in the tree.");
                }
                break;
            case 3:
                System.out.print("Enter the key to search: ");
                int searchKey = scanner.nextInt();
                if (bTree.search(searchKey)) {
                    System.out.println("Key found in the tree.");
                } else {
                    System.out.println("Key not found in the tree.");
                }
                break;
            case 4:
                System.out.println("B-tree:");
                bTree.print();
                break;
            case 5:
                System.out.println("Exiting...");
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }

        System.out.println();
    } while (choice != 5);
}

}
