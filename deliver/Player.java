import java.util.*;
import java.io.*;

public class Player {

    static Random random=new Random();

    static void setupBoardState(MyState state, int player, char[][] board)
    {
        /* Set up the current state */
        state.player = player;
        PlayerHelper.memcpy(state.board,board);

        /* Find the legal moves for the current state */
        PlayerHelper.FindLegalMoves(state);
    }

    
    static void PerformMove(MyState state, int moveIndex)
    {
        PlayerHelper.PerformMove(state.board, state.movelist[moveIndex], PlayerHelper.MoveLength(state.movelist[moveIndex]));
        state.player = state.player%2+1;
        PlayerHelper.FindLegalMoves(state);
    }

    static class Node{
        MyState state;
        Node[] children;
        Node parent;
        double won=0.0;
        int played=0;

        public Node(MyState state, Node parent){
            this.parent = parent;
            this.state = state;
            this.children = new Node[state.numLegalMoves];
        }

        public boolean gameOver(){
            return state.numLegalMoves == 0;
        }

        public boolean isLeaf(){
            return children[0] == null;
        }
    }



    public static void FindBestMove(int player, char[][] board, char[] bestmove, long end) {
        
        MyState state = new MyState(); // , nextstate;
        setupBoardState(state, player, board);

        //shuffle
        // ArrayList<char[]> moves = new ArrayList<>();
        // for (int i = 0; i < state.numLegalMoves; i++) moves.add(state.movelist[i]);
        // Collections.shuffle(moves, new Random());
        // for (int i = 0; i < state.numLegalMoves; i++) state.movelist[i] = moves.get(i);

        Node root = new Node(state, null);
        for (int i = 0; i < state.numLegalMoves; i++){
            MyState nextState = new MyState(root.state);
            PerformMove(nextState, i);
            root.children[i] = new Node(nextState, root);
        }

        while (System.currentTimeMillis() < end){
            MCTS(root, end);
        }

        int bestChoice = root.children[0].played;// - root.children[0].won)/root.played; // proportion won out of total played
        int choiceIndex = 0;
        for (int x = 1; x < root.children.length; x++) {
            int temp = root.children[x].played;// - root.children[x].won)/root.played;
            // System.err.println("CHILD " + x + " WON: " + (root.children[x].played - root.children[x].won) + " PLAYED: " + root.children[x].played);
            if (temp > bestChoice){
                bestChoice = temp;
                choiceIndex = x;
            }
        }
        // System.err.println("ROOT WON: " + root.won + " PLAYED: " + root.played);
        // System.err.println("CHOSE CHILD " + choiceIndex);// + " WON: " + (root.children[choiceIndex].played - root.children[choiceIndex].won) + " PLAYED: " + root.children[choiceIndex].played);
        PlayerHelper.memcpy(bestmove, state.movelist[choiceIndex], PlayerHelper.MoveLength(state.movelist[choiceIndex]));
    }

    // search the current tree to find the node to expand based on utility function 
    static void MCTS(Node node, long end){
        if (System.currentTimeMillis() > end) return;
        if (node.gameOver()){
            node.played++;
            backprop(node.parent, 1.0, 1);
            return;
        }
        if (node.isLeaf()){
            expand(node, end);
            return;
        }

        Double best = utility(node.children[0], node);
        Node selected = node.children[0];
        for(int i=1; i<node.children.length;i++){
            Double temp = utility(node.children[i], node);
            if (temp>best){
                best = temp;
                selected=node.children[i];
            }

        }
        MCTS(selected, end);
    }

    static double playout(MyState state, int moveLimit){
        if (state.numLegalMoves <= 0) return 1.0;
        if (moveLimit <= 0) return 0.0;

        int index = random.nextInt(state.numLegalMoves);
        MyState nextState = new MyState(state);
        PerformMove(nextState, index);
        return -playout(nextState, --moveLimit);
    }

    //backpropogate played values and won(flipping each time) to parent node from expansion of leaf
    static void backprop(Node node){
        backprop(node.parent, node.played - node.won, node.played);
    }

    static void backprop(Node node, double won, int played){
        if (node == null) return;
        node.won+=won;
        node.played+=played;
        backprop(node.parent, played-won, played);
    }

    //expand children of provided node by performing move
    static void expand(Node node, long end){
        for (int i=0; i<node.state.numLegalMoves; i++){
            MyState nextState = new MyState(node.state);
            PerformMove(nextState, i);
            node.children[i] = new Node(nextState, node);
            // double temp = abMove(nextState, 50, end);
            double temp = playout(nextState, 100);
            if (temp > 0) node.won += 1.0;
            // else if (temp == 0) node.won +=0.5;
            node.played++;
            if (System.currentTimeMillis() > end) break;
        }
        backprop(node);
    }

    static double utility(Node child, Node node){
        if (child.played < 1) return 100;
        return (double)((child.played - child.won)/child.played + 1.4*Math.sqrt(Math.log(node.played)/child.played));
    }
}
