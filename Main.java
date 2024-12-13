import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class Main{

	public static void main(String [] args) {

        Scanner in = new Scanner(System.in);
        int N = Integer.parseInt(args[0]);
        int M = Integer.parseInt(args[1]);
		State start= new State('l',N,M);
        int k = Integer.parseInt(args[2]);
        in.close();
		long startTimer = System.currentTimeMillis();
		State finalState = aStar.run(start,k);
		long end = System.currentTimeMillis();
		if(finalState == null) System.out.println("Could not find a solution.");
        else {
            State temp = finalState;
            ArrayList<State> tree = new ArrayList<>();
			tree.add(finalState);
            while(temp.getFather() != null) {
                tree.add(temp.getFather());
                temp = temp.getFather();
            }
            Collections.reverse(tree);
            for(State node: tree) System.out.println(node);
            System.out.println();
            System.out.println("Time: " + (double)(end - startTimer) / 1000 + " sec.");
        }
	}
	
}
