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
            return children.length == 0;
        }

        public boolean isLeaf(){
            return children[0] == null;
        }
    }



    public static void FindBestMove(int player, char[][] board, char[] bestmove, long end) {
        
        MyState state = new MyState(); // , nextstate;
        setupBoardState(state, player, board);

        //shuffle
        ArrayList<char[]> moves = new ArrayList<>();
        for (int i = 0; i < state.numLegalMoves; i++) moves.add(state.movelist[i]);
        Collections.shuffle(moves, new Random());
        for (int i = 0; i < state.numLegalMoves; i++) state.movelist[i] = moves.get(i);

        Node root = new Node(state, null);

        while (System.currentTimeMillis() < end){
            MCTS(root, end);
        }

        double bestChoice = (root.children[0].played - root.children[0].won)/root.played; // proportion won out of total played
        int choiceIndex = 0;
        for (int x = 1; x < root.children.length; x++) {
            double temp = (root.children[x].played - root.children[x].won)/root.played;
            // System.err.println("CHILD " + x + " WON: " + (root.children[x].played - root.children[x].won) + " PLAYED: " + root.children[x].played + " SCORE: " + temp);
            if (temp > bestChoice){
                bestChoice = temp;
                choiceIndex = x;
            }
        }
        // System.err.println("WON: " + root.won + " PLAYED: " + root.played);
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

    // Alpha beta pruning in minimax (simulation/playout)
    // public static double abMove(MyState state, int moveLimit, long end) {
    //     // if (System.currentTimeMillis() > end) return 0;

    //     int myBestMoveIndex = 0;
    //     double moveVal = Double.NEGATIVE_INFINITY;

    //     if (state.numLegalMoves <= 0) return 1.0;
    //     if (moveLimit <= 0) return 0.5;
        
    //     //shuffle
    //     ArrayList<char[]> moves = new ArrayList<>();
    //     for (int i = 0; i < state.numLegalMoves; i++) moves.add(state.movelist[i]);
    //     Collections.shuffle(moves, new Random());
    //     for (int i = 0; i < state.numLegalMoves; i++) state.movelist[i] = moves.get(i);

    //     for (int x = 0; x < state.numLegalMoves; x++) {
    //         if (System.currentTimeMillis() > end) return 0.5;
    //         MyState nextState = new MyState(state);
    //         PerformMove(nextState, x);
    //         // printBoard(nextState);
    //         // System.err.println("Eval of board: " + evalBoard(nextState));
    //         double temp = min(nextState, moveVal, Double.POSITIVE_INFINITY, 3);
    //         if(temp > moveVal){
    //             moveVal = temp;
    //             myBestMoveIndex = x;
    //         }

    //     }
    //     MyState nextState = new MyState(state);
    //     PerformMove(nextState, myBestMoveIndex);
    //     return -abMove(nextState, --moveLimit, end);
    // }

    static double playout(MyState state, int moveLimit){
        if (state.numLegalMoves <= 0) return 1.0;
        if (moveLimit <= 0) return 0.5;

        int index = random.nextInt(state.numLegalMoves);
        MyState nextState = new MyState(state);
        PerformMove(nextState, index);
        return - playout(nextState, --moveLimit);
    }

    // static double min(MyState state, double alpha, double beta, int depth){
    //     if (depth<=0 || state.numLegalMoves <= 0) return 1/evalBoard(state);
    //     depth--;

    //     for (int x = 0; x <state.numLegalMoves; x++){
    //         MyState nextState = new MyState(state);
    //         PerformMove(nextState, x);

    //         beta = Math.min(beta, max(nextState, alpha, beta, depth));

    //         if (beta <= alpha) return alpha;
    //     }
    //     return beta;
    // }

    // static double max(MyState state, double alpha, double beta, int depth){
    //     if (depth<=0 || state.numLegalMoves <= 0) return evalBoard(state);
    //     depth--;

    //     for (int x = 0; x <state.numLegalMoves; x++){
    //         MyState nextState = new MyState(state);
    //         PerformMove(nextState, x);

    //         alpha = Math.max(alpha, min(nextState, alpha, beta, depth));

    //         if (alpha >= beta) return beta;
    //     }
    //     return alpha;
    // }

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
            else if (temp == 0) node.won +=0.5;
            node.played++;
            if (System.currentTimeMillis() > end) break;
        }
        backprop(node);
    }

    static double utility(Node child, Node node){
        if (child.played < 1) return 100;
        return (double)((child.played - child.won)/child.played + 1.4*Math.sqrt(Math.log(node.played)/child.played));
    }

    // static void printBoard(MyState state)
    // {
    //     int y,x;

    //     for(y=0; y<8; y++) 
    //     {
    //         for(x=0; x<8; x++)
    //         {
    //             if(x%2 != y%2)
    //             {
    //                  if(PlayerHelper.empty(state.board[y][x]))
    //                  {
    //                      System.err.print(" ");
    //                  }
    //                  else if(PlayerHelper.king(state.board[y][x]))
    //                  {
    //                      if(PlayerHelper.color(state.board[y][x])==2) System.err.print("B");
    //                      else System.err.print("A");
    //                  }
    //                  else if(PlayerHelper.piece(state.board[y][x]))
    //                  {
    //                      if(PlayerHelper.color(state.board[y][x])==2) System.err.print("b");
    //                      else System.err.print("a");
    //                  }
    //             }
    //             else
    //             {
    //                 System.err.print("@");
    //             }
    //         }
    //         System.err.print("\n");
    //     }
    // }

    /* An example of how to walk through a board and determine what pieces are on it*/
    // static double evalBoard(MyState state)
    // {
    //     int y,x;
    //     double score;
    //     score=0.0;

    //     for(y=0; y<8; y++) for(x=0; x<8; x++)
    //     {
    //         if(x%2 != y%2)
    //         {
    //              if(PlayerHelper.empty(state.board[y][x]))
    //              {
    //              }
    //              else if(PlayerHelper.king(state.board[y][x]))
    //              {
    //                  if(PlayerHelper.color(state.board[y][x])==2) score += 2.0;
    //                  else score -= 2.0;
    //              }
    //              else if(PlayerHelper.piece(state.board[y][x]))
    //              {
    //                  if(PlayerHelper.color(state.board[y][x])==2) score += 1.0;
    //                  else score -= 1.0;
    //              }
    //         }
    //     }

    //     if(state.player==1) score = -score;

    //     return score;

    // }

    // static double evalBoard(MyState state)
    // {
    //     int y,x;
    //     double score, p1Pieces, p2Pieces, p1Rat, p2Rat;
    //     int  p1Cluster, p2Cluster, p1BackPawn, p2BackPawn;
    //     ArrayList<Integer> p1X, p1Y, p2X, p2Y;
    //     p1X = new ArrayList<>();
    //     p1Y = new ArrayList<>();
    //     p2X = new ArrayList<>();
    //     p2Y = new ArrayList<>();
    //     score = 0.0;
    //     p1Pieces = 0.0;
    //     p2Pieces = 0.0;
    //     p1BackPawn = 0;
    //     p2BackPawn = 0;

    //     for(y=0; y<8; y++) for(x=0; x<8; x++)
    //     {
    //         if(x%2 != y%2)
    //         {
    //              if(PlayerHelper.empty(state.board[y][x]))
    //              {
    //              }
    //              else if(PlayerHelper.king(state.board[y][x]))
    //              {
    //                  if(PlayerHelper.color(state.board[y][x])==2){
    //                     p2Pieces += 5.0;
    //                     p2X.add(x);
    //                     p2Y.add(y);
    //                  } else {
    //                     p1Pieces += 5.0;
    //                     p1X.add(x);
    //                     p1Y.add(y);
    //                  }
    //              }
    //              else if(PlayerHelper.piece(state.board[y][x]))
    //              {
    //                  if(PlayerHelper.color(state.board[y][x])==2){
    //                     if (y == 0) p2BackPawn+=1;
    //                     p2Pieces += 3.0;
    //                     p2X.add(x);
    //                     p2Y.add(y);
    //                  } else {
    //                     if (y == 8) p1BackPawn+=1;
    //                     p1Pieces += 3.0;
    //                     p1X.add(x);
    //                     p1Y.add(y);
    //                  }
    //            }
    //         }
    //     }

    //     double totalPieces = p1Pieces + p2Pieces;
    //     p2Rat = p1Pieces!=0 ? p2Pieces/p1Pieces : Double.MAX_VALUE;
    //     p1Rat = p2Pieces!=0 ? p1Pieces/p2Pieces: Double.MAX_VALUE;

    //     //pairwise manhattan distance between pieces
    //     p1Cluster = 0;
    //     for (int i = 0; i < p1X.size() - 1; i++){
    //         for (int j = i+ 1; j < p1X.size(); j++){
    //             p1Cluster += (Math.abs(p1X.get(i) - p1X.get(j)) + Math.abs(p1Y.get(i) - p1Y.get(j)));
    //         }
    //     }
    //     p2Cluster = 0;
    //     for (int i = 0; i < p2X.size() - 1; i++){
    //         for (int j = i+ 1; j < p2X.size(); j++){
    //             p2Cluster += (Math.abs(p2X.get(i) - p2X.get(j)) + Math.abs(p2Y.get(i) - p2Y.get(j)));
    //         }
    //     }
    //     //divde by at least number of pieces yielding a cluster coefficient (otherwise less pieces -> more cluster value)
    //     p1Cluster = (int)(p1Cluster/p1Pieces);
    //     p2Cluster = (int)(p2Cluster/p1Pieces); 

        
    //     double trade1 = 0.0;
    //     double trade2 = 0.0;
    //     if (p1Pieces < 7.0 && p1Rat > 1.0) trade1 = -p1Pieces;
    //     if (p2Pieces < 7.0 && p2Rat > 1.0) trade2 = -p2Pieces;

    //     double p1Score = Math.floor(p1Rat*1000) + p1BackPawn -p1Cluster + trade1;
    //     double p2Score = Math.floor(p2Rat*1000) + p2BackPawn -p2Cluster + trade2;

    //     score = p2Score/p1Score;
    //     if(state.player==1) score = 1/score;   

    //     return score;

    // }


}
