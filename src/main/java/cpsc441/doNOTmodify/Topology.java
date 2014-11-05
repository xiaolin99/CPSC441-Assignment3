package main.java.cpsc441.doNOTmodify;
import java.io.File;
import java.util.Arrays;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * A <code>Topology</code> instance is used to load the initial costs from a
 * file. See the in-line comments for the file format.
 */
public class Topology {
	private int[][] weights;
	private int[][] changedWeights;
	private int[] changedIndex;
	private int numRouters = 0;
	private int changeTime = 0;
    private boolean hasTwoParts = false;
	
	public Topology(File file) throws FileNotFoundException {
        hasTwoParts = false;
		weights = readWeightFile(file);
		if (hasTwoParts)
            FindAffectedRouters();
	}

	public int getNumRouters(){
		return numRouters;
	}
	
	public int getChangeTime()
	{
		return changeTime;
	}
	
	private int[][] readWeightFile(File file) throws FileNotFoundException {
		Scanner scanner = new Scanner(file);
		// The first line contains an integer n, the number of routers
		this.numRouters = scanner.nextInt();
		int weigths[][] = new int[numRouters][numRouters];

		// The rest of the file contains an nxn matrix of integers containing
		// space separated costs between the routes. The item at row i, column j
		// contains the cost between router i and j.
		for (int i = 0; i < weigths.length; i++) {
			for (int j = 0; j < weigths[i].length; j++) {
				weigths[i][j] = scanner.nextInt();
			}
		}

        changedWeights = new int[numRouters][numRouters];
        changedIndex = new int[numRouters];

        //
        // MG: No need to handle link cost changes
        //
        /*
		if (scanner.hasNextInt())
        {
            hasTwoParts = true;
            this.changeTime = scanner.nextInt();
		
		    for (int i = 0; i < changedWeights.length; i++) {
			    for (int j = 0; j < changedWeights[i].length; j++) {
				    changedWeights[i][j] = scanner.nextInt();
			    }
		    }
        }
        */
        
		scanner.close();
		return weigths;
	}

	private void FindAffectedRouters()
	{
		Arrays.fill(changedIndex,-1);
		for(int i = 0; i < numRouters; i++)
		{
			for (int j = 0; j < numRouters; j++)
			{
				if (weights[i][j] != changedWeights[i][j])
				{
					changedIndex[i] = 1;
					break;
				}
			}
		}
	}
	
	public int[] getWeightsForRouter(int routerId) {
		// clone the array to prevent unwanted modifications outside the class
		return weights[routerId].clone();
	}
	
	public int[] getChangedWeightsForRouter(int routerId){
		return changedWeights[routerId].clone();
	}
	
	public int[] getChangedIndex()
	{
		return changedIndex.clone();
	}
	
	public int[][] getWeightTable() {
		// clone the array to prevent unwanted modifications outside the class
		return HelperUtils.deepClone2DArray(weights);
	}

	public int getWeight(int from, int to) {
		return weights[from][to];
	}

    public void changeWeights()
    {
        for (int i = 0; i < numRouters; i++)
        {
            for(int j = 0; j < numRouters; j++)
            {
                weights[i][j] = changedWeights[i][j];
            }
        }
    }

    public boolean  getHasTwoParts()
    {
       return hasTwoParts;
    }

	public static void main(String[] args) throws FileNotFoundException {
		Topology t1 = new Topology(new File("topology.txt"));
		//int[] i = t1.getChangedWeightsForRouter(1);
		if (t1.getHasTwoParts())
        {
            t1.changeWeights();

            int[] in2 = t1.getChangedIndex();

            for (int k : in2)
                System.out.printf(k + " ");

            System.out.println("\n-------------------------------------");

            for(int i = 0; i < t1.getNumRouters(); i++)
            {
                for(int j = 0; j < t1.getNumRouters(); j++)
			        System.out.printf(t1.getWeight(i,j) + "\t");

                System.out.println();
            }
        }
	}
}