import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by rkalhans on 8/4/2016.
 */

public class Player
{
    static Score ExecutorScores[];

    public static class ScoreComparator implements Comparator<Score>
    {
        @Override
        public int compare(Score x, Score y)
        {
            if(x == null && y == null) return 0;
            if(x == null) return 1;
            if(y == null) return -1;
            if (x.score < y.score)
            {
                return 1;
            }
            if (x.score > y.score)
            {
                return -1;
            }
            return 0;
        }
    }
     private static class Score<T> implements Comparable<Score>{
         double score;
         Object output;
         T chromosome;
         Score (double _score, Object _output, T chromosome)
         {
             this.score = _score;
             this.output = _output;
             this.chromosome = chromosome;
         }

         @Override
         public int compareTo(Score s) {
             if(s == null) return 1;
             if(s.score > this.score) return -1;
             if(s.score == this.score) return 0;
             return 1;
         }
     }
     static class GeneticAlgo <T extends Chromosome> {
         private static final double WAITTIME_PERCENTAGE = 0.95;
         private final ExecutorService pool;
        private final int timeoutMs;
        public GeneticAlgo(int poolSize, int timeoutMs) {
            pool = Executors.newFixedThreadPool(poolSize);
            this.timeoutMs = timeoutMs;
            ExecutorScores = new Score[poolSize];
        }
        public void start(List<T>chromosomesList, List<CrossoverAlgo<T>> algos) {


            Thread t = new Thread(new Scheduler(chromosomesList, algos));
            t.start();
            try {
                Thread.sleep((int)(timeoutMs*WAITTIME_PERCENTAGE));
            } catch (InterruptedException e) {
                // interrupted by the scheduler
            } finally {
                // well we are out of time. Get the best value from the chromosome.
                double max = Double.MIN_VALUE;
                Score outputScore= ExecutorScores[0];
                // output the best result after the timeout
                for(Score s : ExecutorScores){
                    if(s.score > max ){
                        max = s.score;
                        outputScore = s;
                    }
                }
                System.out.println(outputScore.chromosome.toString());
                try {
                    List<Runnable> threads = pool.shutdownNow();
                    pool.awaitTermination(10, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        class Scheduler implements Runnable {

            List<T> chromomome ;
            List<CrossoverAlgo<T>> algos;
            public Scheduler(List<T> chromosomesList, List<CrossoverAlgo<T>> algos){
                this.chromomome = chromosomesList;
                this.algos = algos;
            };

            @Override
            public void run() {
            	int idx =0;
              for(CrossoverAlgo<T> algo: algos)
              {
                  pool.submit(new GAExecutor<T>(idx++, chromomome, algo));
              }
            }
        }
    }
    public static class GAExecutor<T extends Chromosome>implements Runnable{
        int id;
        List<T>chromosomes;
        PriorityQueue<Score<T>> scores = new PriorityQueue<>(500, new ScoreComparator());
        CrossoverAlgo<T> algo;

        public GAExecutor(int id, List<T>chromosomes, CrossoverAlgo<T> algo){
            this.chromosomes = chromosomes;
            this.id = id;
            this.algo = algo;
        }

        @Override
        public void run(){
            while (!Thread.currentThread().isInterrupted()){

                for (T c : chromosomes)
                {
                    scores.add(c.GetFitness());
                }
                Score highScore =  scores.peek();
                if(highScore.compareTo(ExecutorScores[id]) == 1)
                {
                    ExecutorScores[id] = scores.peek();
                }
                chromosomes = algo.GetListOfCrossovers(GetTopChromosomes(100));
                scores.clear();
            }
        }

        private List<T>GetTopChromosomes(int N){
            int i = 0;
            List<T> chromosomes = new ArrayList(N);
            for(Score<T> s : scores){
                i++;
                chromosomes.add(s.chromosome);
                if(i >= N) break;

            }
            return chromosomes;
        }

    }

    public static abstract class Chromosome {
        public abstract Score GetFitness();
    }

    public static abstract class CrossoverAlgo<T>{
        abstract List<T> GetListOfCrossovers(List<T> chromosomes);
    }

    //---------------------------------------------------------------------------------------------------//

    /*
    * HERE WE WOULD FIND THE gLOBAL MAXIMA OF A MATHEMATICAL GRAPH GIVEN BY Y = f(X).
    * THE FITNESS FUNCTION IS THE VALUE OF Y; HIGHER THE Y BETTER THE SOLUTION IS
    * HOWEVER IF WE ONLY CONSIDER THE BEST VALUE TILL NOW WE WILL END UP WITH LOCAL MAXIMA AND HERE IS WHERE THE ga COMES INTO PLAY.
    *
    */
    public static class GraphChromosome extends Chromosome{
        double  x;
        public GraphChromosome(double _x){
            this.x = _x;
        }
        @Override
        public Score GetFitness() {

            double fitnessScore= Double.MIN_VALUE;
            if((x >= 3) && (x<=4)){
                fitnessScore =  (x * x) + 11 - (6 * x);}
            if((x > 4) && (x<=5))
                fitnessScore = x-1;
            if(x > 5 && x <= 6 )
                fitnessScore = 9-x;
            if(x> 6 && x < (6+Math.PI))
                fitnessScore = (2*Math.sin(x-6))+3;

            return new Score(fitnessScore, x, this);
        }

        public String toString(){
          return "x: "+x + " y: "+GetFitness().score;
        }
    }

    public static class GraphCrossOverAlgo extends CrossoverAlgo<GraphChromosome> {

        @Override
        List<GraphChromosome> GetListOfCrossovers(List<GraphChromosome> chromosomes) {
            List <GraphChromosome> crossChromosomes = new ArrayList();
            // add the parents by default.
            crossChromosomes.addAll(chromosomes);
            for(int i = 0; i< chromosomes.size(); i++)
            {
                for(int j = i+1; j< chromosomes.size(); j++) {
                    crossChromosomes.add(new GraphChromosome((chromosomes.get(i).x+chromosomes.get(j).x)/2));
                    crossChromosomes.add(new GraphChromosome((chromosomes.get(i).x-chromosomes.get(j).x)/2));
                    crossChromosomes.add(new GraphChromosome(chromosomes.get(i).x+(chromosomes.get(i).x+chromosomes.get(j).x)/2));
                    crossChromosomes.add(new GraphChromosome(chromosomes.get(i).x-(chromosomes.get(i).x+chromosomes.get(j).x)/2));
                }
            }
            return crossChromosomes;
        }
    }

    public static void main(String[] args) {
        List<GraphChromosome> list = new ArrayList<>(2);
        list.add(new GraphChromosome(0.10));
        list.add(new GraphChromosome(1.3));
        
        List<CrossoverAlgo<GraphChromosome>> algos =  new ArrayList<CrossoverAlgo<GraphChromosome>>();
        algos.add(new GraphCrossOverAlgo());
        GeneticAlgo<GraphChromosome> GA = new GeneticAlgo<>(1, 100);
        GA.start(list,algos );
    }
}
