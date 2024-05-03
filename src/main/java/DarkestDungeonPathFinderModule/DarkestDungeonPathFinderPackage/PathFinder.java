
package DarkestDungeonPathFinderPackage;
   
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PathFinder
{

   private final ExecutorService PARALLEL = Executors.newWorkStealingPool();

   private record Node(String id)
   {
   
      Node
      {
      
         Objects.requireNonNull(id);
      
      }
   
      public String toString()
      {
      
         return this.id;
      
      }
   
   }

   //All Paths are bidirectional
   //Use Path::exactMatch if you want to ensure 2 paths are not just equals but same order
   private record Path(Node start, Node end, int weight)
   {
   
      private static final Pattern REGEX = Pattern.compile("^([a-zA-Z](?:,|)[a-zA-Z](?:,|)[0-9]+|)$");
   
      Path
      {
      
         Objects.requireNonNull(start);
         Objects.requireNonNull(end);
      
      }
   
      public static Path of(String potentialPath)
      {
      
         Objects.requireNonNull(potentialPath);
      
         if (!isValidPath(potentialPath))
         {
         
            throw new IllegalArgumentException("Invalid path! potentialPath = " + potentialPath);
         
         }
      
         final String[] array =
            potentialPath
               .split(
                     potentialPath.contains(",")
                     ?
                     ","
                     :
                     ""
                     );
      
         final Node start = new Node(array[0]);
         final Node end   = new Node(array[1]);
         final int length = Integer.parseInt(array[2]);
      
         return new Path(start, end, length);
      
      }
   
      public static boolean isValidPath(String potentialPath)
      {
      
         return REGEX.matcher(potentialPath).matches();
      
      }
   
      public boolean startsWith(Node node)
      {
      
         return this.start.equals(node);
      
      }
   
      public boolean endsWith(Node node)
      {
      
         return this.end.equals(node);
      
      }
   
      public boolean has(Node node)
      {
      
         return this.startsWith(node) || this.endsWith(node);
      
      }
   
      public Path flip()
      {
      
         return new Path(this.end, this.start, this.weight);
      
      }
   
      public boolean canConnectTo(Path other)
      {
      
         Objects.requireNonNull(other);
      
         return
            this.start.equals(other.start)
            ||
            this.start.equals(other.end)
            ||
            this.end.equals(other.start)
            ||
            this.end.equals(other.end)
            ;
      
      }
   
      @Override
      public boolean equals(Object other)
      {
      
         if (other == null)
         {
         
            return false;
         
         }
         
         else if (other instanceof Path p)
         {
         
            if (p.start.equals(this.start) && p.end.equals(this.end))
            {
            
               return true;
            
            }
            
            else if (p.start.equals(this.end) && p.end.equals(this.start))
            {
            
               return true;
            
            }
            
            else
            {
            
               return false;
            
            }
         
         }
      
         return false;
      
      }
   
      public boolean exactMatch(Object other)
      {
      
         if (other == null)
         {
         
            return false;
         
         }
      
         if (other instanceof Path p && p.start.equals(this.start) && p.end.equals(this.end) && p.weight == this.weight)
         {
         
            return true;
         
         }
      
         return false;
      
      }
   
      public String toString()
      {
      
         return "" + this.start + this.end + this.weight;
      
      }
   
   }

   private record Chain(List<Path> chain) implements Comparable<Chain>
   {
   
      public static final Comparator<Chain> comparator =
         Comparator
            .comparing(Chain::weight)
            .reversed()
            .thenComparing(Chain::toString)
            .reversed()
            ;
   
      Chain
      {
      
         chain = sanitize(chain);
      
      }
   
      public Chain(Path... chain)
      {
      
         this(List.of(chain));
      
      }
   
      public static Chain of(Path... chain)
      {
      
         return new Chain(chain);
      
      }
   
      public static Chain of(Chain original, Path... chain)
      {
      
         final var copy = new ArrayList<>(original.chain());
         copy.addAll(List.of(chain));
      
         return new Chain(copy);
      
      }
   
      @Override
      public int compareTo(Chain other)
      {
      
         return comparator.compare(this, other);
      
      }
   
      public int weight()
      {
      
         return
            this.chain
               .stream()
               .mapToInt(Path::weight)
               .sum()
               ;
      
      }
   
      private static List<Path> sanitize(List<Path> chain)
      {
      
         Objects.requireNonNull(chain);
      
         for (int i = 1; i < chain.size(); i++)
         {
            
            final Path previous = chain.get(i - 1);
            final Path current  = chain.get(i);
            
            if (!previous.end().equals(current.start))
            {
               
               throw new IllegalArgumentException("Link not connected to the previous link on the chain! previous = "
                                    + previous + " current = " + current);
               
            }
            
         }
         
         return List.copyOf(chain);
      
      }
   
      public Chain add(Path current)
      {
      
         Objects.requireNonNull(current);
      
         final List<Path> elements = new ArrayList<>(this.chain);
      
         if (elements.isEmpty())
         {
         
            elements.add(current);
         
            return new Chain(elements);
         
         }
      
         final Path previous = elements.get(elements.size() - 1);
      
         if (previous.end().equals(current.start()))
         {
         
            elements.add(current);
         
         }
         
         else if (previous.end().equals(current.end()))
         {
         
            elements.add(current.flip());
         
         }
         
         else
         {
         
            throw new IllegalArgumentException("Link not connected to the previous link on the chain! previous = "
                                 + previous + " current = " + current);
         
         }
      
         return new Chain(elements);
      
      }
   
      public int size()
      {
      
         return this.chain.size();
      
      }
   
      public boolean contains(Node node)
      {
      
         Objects.requireNonNull(node);
      
         for (Path each : this.chain)
         {
         
            if (each.has(node))
            {
            
               return true;
            
            }
         
         }
      
         return false;
      
      }
   
      public boolean contains(Path path)
      {
      
         Objects.requireNonNull(path);
      
         for (Path each : this.chain)
         {
         
            if (each.equals(path))
            {
            
               return true;
            
            }
         
         }
      
         return false;
      
      }
   
      public boolean isEmpty()
      {
      
         return this.chain.isEmpty();
      
      }
   
      public boolean isWithinBoundsOf(GameMap gameMap)
      {
      
         return this.size() <= gameMap.maxNumberOfSteps();
      
      }
   
      public static Chain empty()
      {
      
         return new Chain(List.of());
      
      }
   
      public boolean shouldAddMore()
      {
      
         if (this.chain.isEmpty())
         {
         
            return true;
         
         }
      
         Map<Path, Long> wat =
            this.chain
               .stream()
               .collect(
                       Collectors.groupingBy(
                                         Function.identity(),
                                         Collectors.counting()
                                         )
                       );
      
         long maxPathFrequency =
            wat
               .entrySet()
               .stream()
               .max(
                   Comparator.comparing(
                                     Map.Entry::getValue
                                     )
                   )
               .orElseThrow()
               .getValue()
               ;
      
      
         return maxPathFrequency < 3;
      
      }
   
      public int countOf(Node node)
      {
      
         Objects.requireNonNull(node);
      
         int count = 0;
      
         for (Path each : this.chain)
         {
         
            if (each.endsWith(node))
            {
            
               count++;
            
            }
         
         }
      
         return count;
      
      }
   
      public Node lastNode()
      {
      
         return this.chain().get(this.chain.size() - 1).end();
      
      }
   
   }

   private record UncheckedChain(List<Path> chain)
   {
   
      public int weight()
      {
      
         return
            this.chain
               .stream()
               .mapToInt(Path::weight)
               .sum()
               ;
      
      }
   
   }

   private record GameMap(List<Path> gameMap)
   {
   
      GameMap
      {
      
         Objects.requireNonNull(gameMap);
         gameMap = sanitize(gameMap);
      
      }
   
      public GameMap(Path... gameMap)
      {
      
         this(List.of(gameMap));
      
      }
   
      public GameMap(String listOfPaths)
      {
      
         // this
      //       (
      //          Stream
      //             .of(listOfPaths.split("_"))
      //
      //       );
      
         this(
               listOfPaths
                  .lines()
                  .filter(Path::isValidPath)
                  .map(Path::of)
                  .toList()
               );
      
      }
   
      public boolean contains(Path path)
      {
      
         Objects.requireNonNull(path);
      
         return this.gameMap.contains(path) || this.gameMap.contains(path.flip());
      
      }
   
      public boolean contains(Node node)
      {
      
         Objects.requireNonNull(node);
      
         for (Path each : this.gameMap)
         {
         
            if (each.has(node))
            {
            
               return true;
            
            }
         
         }
      
         return false;
      
      }
   
      public int count(Node node)
      {
      
         Objects.requireNonNull(node);
      
         int count = 0;
      
         for (Path each : this.gameMap)
         {
         
            if (each.has(node))
            {
            
               count++;
            
            }
         
         }
      
         return count;
      
      }
   
      public int totalWeight()
      {
      
         int totalWeight = 0;
      
         for (Path each : this.gameMap)
         {
         
            totalWeight+= each.weight();
         
         }
      
         return totalWeight;
      
      }
   
      public int maxPossibleTraversalWeight()
      {
      
         return this.totalWeight() * 2;
      
      }
   
      public int maxNumberOfSteps()
      {
      
         return this.gameMap.size() * 2;
      
      }
   
      public List<Path> findPathsFromNodeOptimized(Node node, Chain chain)
      {
      
         Objects.requireNonNull(node);
         Objects.requireNonNull(chain);
      
         final List<Path> paths = new ArrayList<>();
      
         for (Path each : this.gameMap)
         {
         
            if (each.startsWith(node))
            {
            
               paths.add(each);
            
            }
            
            else if (each.endsWith(node))
            {
            
               paths.add(each.flip());
            
            }
         
         }
      
         final Comparator<? super Path> tempComparator =
            Comparator
               .comparingInt
               (
                  path -> chain.countOf(((Path)path).end())
               )
               .thenComparingInt
               (
                  path -> this.distanceFromNodeViaPathToClosestPathNotOnChain(chain, (Path)path, ((Path)path).start())
               );
      
         Collections
            .sort
               (
               paths,
               tempComparator
               );
      
         return paths;
      
      }
   
      public List<Path> findPathsFromNode(Node node)
      {
      
         Objects.requireNonNull(node);
      
         final List<Path> paths = new ArrayList<>();
      
         for (Path each : this.gameMap)
         {
         
            if (each.startsWith(node))
            {
            
               paths.add(each);
            
            }
            
            else if (each.endsWith(node))
            {
            
               paths.add(each.flip());
            
            }
         
         }
      
         return paths;
      
      }
   
      public boolean isSubsetOf(Chain chain)
      {
      
         Objects.requireNonNull(chain);
      
         final var chainPaths = chain.chain();
         final var gameMapPaths = this.gameMap();
      
         final boolean result = chainPaths.containsAll(gameMapPaths);
      
         return result;
      
      }
   
      public static List<Path> sanitize(List<Path> gameMap)
      {
      
         Objects.requireNonNull(gameMap);
      
         final Set<Node> nodes = new HashSet<>();
      
         for (Path path : gameMap)
         {
         
            if (GameMap.pathContainedWithinMap(gameMap, path))
            {
            
               nodes.add(path.start());
               nodes.add(path.end());
            
            }
            
            else
            {
            
               throw new IllegalArgumentException("Every path you add must be either directly or indirectly connected to all other paths! paths = "
                                    + gameMap + " path = " + path);
            
            }
         
         }
      
         return List.copyOf(gameMap);
      
      }
   
      private static boolean pathContainedWithinMap(List<Path> originalList, Path path)
      {
      
         final List<Path> paths = new ArrayList<>(originalList);
         final boolean didRemove = paths.remove(path);
      
         if (didRemove)
         {
         
            return
               paths
                  .stream()
                  .anyMatch(path::canConnectTo)
                  ;
         
         }
         
         else
         {
         
            return false;
         
         }
      
      }
   
      private int distanceFromNodeViaPathToClosestPathNotOnChain(Chain chain, Path path, Node node)
      {
      
         if (!path.start().equals(node))
         {
         
            throw new IllegalArgumentException("Start must equal the node!");
         
         }
      
         final Set<Chain> searchChains = this.findFirstUntraveledPath(chain, path, Chain.empty(), new HashSet<>());
      
         final int min =
            searchChains
               .stream()
               .min(Comparator.comparingInt(Chain::weight))
               .orElse(Chain.empty())
               .size()
               ;
      
         return min;
      
      }
   
      private Set<Chain> findFirstUntraveledPath(Chain chain, Path path, Chain searchChain, Set<Chain> searchChains)
      {
      
         final Node node = path.end();
      
         final List<Path> paths = this.findPathsFromNode(node);
      
         for (Path eachPath : paths)
         {
         
            final Chain nextLink = Chain.of(searchChain, eachPath);
         
            if (!searchChain.contains(eachPath) && !chain.contains(eachPath))
            {
            
               searchChains.add(nextLink);
               return searchChains;
            
            }
            
            else if (!searchChain.contains(eachPath))
            {
            
               searchChains.addAll(this.findFirstUntraveledPath(chain, eachPath, nextLink, searchChains));
            
            }
         
         }
      
         return searchChains;
      
      }
   
      private int amountOfUntraveledWeightLeft(Chain chain)
      {
      
         final var copy = new ArrayList<>(this.gameMap);
      
         copy.removeAll(chain.chain());
      
         return new UncheckedChain(copy).weight();
      
      }
   
      public String toString()
      {
      
         return
            this.gameMap
               .stream()
               .map(Path::toString)
               .collect(Collectors.joining(" -- "))
               ;
      
      }
   
   }

   private record PathFinderInputs(String directions, String node)
   {
   
      PathFinderInputs
      {
      
         Objects.requireNonNull(directions);
         Objects.requireNonNull(node);
      
      }
   
      private PathFinderInputs(String input)
      {
      
         this
               (
               input
                  .substring(input.indexOf('_') + 4, input.lastIndexOf('_'))
                  ,
               input
                  .substring(input.lastIndexOf('_') + 1, input.lastIndexOf('.'))
               );
      
      }
   
      public PathFinderInputs(File file)
      {
      
         this(PathFinder.validateFileName(file).getName());
      
      }
   
      public GameMap generateGameMap()
      {
      
         final String[] splitDirections = this.directions.split("_");
         final String directionsWithNewLines = String.join("\n", splitDirections);
      
         return new GameMap(directionsWithNewLines);
      
      }
   
      public int length()
      {
      
         return directions.length();
      
      }
   
   }

   public void kickOffFileChooser()
   {
   
      final JFileChooser fileChooser = new JFileChooser("./");
      
      fileChooser.showOpenDialog(null);
      
      final File fileToRun = fileChooser.getSelectedFile();
      
      if (fileToRun != null)
      {
      
         this.kickOffAll(fileToRun);
      
      }
   
   }

   public void kickOffAll()
   {
   
      //sleep(20);
   
      var absolutePath = java.nio.file.Path
                  .of("./")
                  .toAbsolutePath();
   
      while (!absolutePath.endsWith("HelperFunctions"))
      {
      
         absolutePath = absolutePath.getParent();
      
      }
   
      absolutePath = Paths.get(absolutePath.toString(), "src", "main", "resources", "DarkestDungeon");
   
      System.out.println(absolutePath);
      var file = absolutePath
                  .toFile();
   
      System.out.println(file);
      var array = file
                  .listFiles()
                  ;
   
      if (array.length == 0)
      {
      
         throw new IllegalStateException("There are no files to run!");
      
      }
   
      kickOffAll(array);
   
   }
   
   private void kickOffAll(final File... array)
   {
   
      final List<PathFinderInputs> list =
         Arrays
            .asList(array)
            .stream()
            .filter(each -> each.isFile())
            .filter(PathFinder::startsWithDate)
            .map(PathFinderInputs::new)
            //.sorted(Comparator.comparingInt(PathFinderInputs::length))
            .peek(System.out::println)
            .toList()
            ;
   
      list.forEach(this::performSingleRun);
   
      //sleep(20);
   
   }

   private static void sleep(int seconds)
   {
   
      try
      {
      
         System.out.println("Waiting");
         Thread.sleep(seconds * 1000);
         System.out.println("Continuing");
      
      }
      
      catch (Throwable e)
      {
      
         throw new RuntimeException(e);
      
      }
   
   }

   private void performSingleRun(PathFinderInputs inputs)
   {
   
      System.out.println();
   
      System.out.println(inputs.node() + " -- " + inputs.generateGameMap());
   
      singleManualRun:
      {
      
         final GameMap gameMap = inputs.generateGameMap();
      
         final Node startingNode = new Node(inputs.node());
      
         final double start = System.currentTimeMillis();
      
         final var result = findBestPathsStartingFrom(startingNode, gameMap);
      
         final double finish = System.currentTimeMillis();
      
         System.out.println("Finished in " + ((finish - start)/1000) + " seconds");
      
         prettyPrintBestChain(result);
      
         System.out.println("FINAL = " + result.weight());
      
      }
   
   }

   private static File validateFileName(File file)
   {
   
      Objects.requireNonNull(file);
   
      if (PathFinder.startsWithDate(file))
      {
      
         return file;
      
      }
      
      else
      {
      
         throw new IllegalArgumentException("Filename is invalid! file = " + file);
      
      }
   
   }

   private static boolean startsWithDate(File file)
   {
   
      Objects.requireNonNull(file);
   
      final String[] fileNameComponents = file.getName().split("_");
   
      return fileNameComponents[0].matches("\\d{8}");
   
   }

   private static boolean startsWithDate(String file)
   {
   
      Objects.requireNonNull(file);
   
      final String[] fileNameComponents = file.split("_");
   
      return fileNameComponents[0].matches("\\d{8}");
   
   }

   private static void prettyPrintBestChain(Chain chain)
   {
   
      final List<Path> list = chain.chain();
   
      System.out.print("\tresult weight = " + chain.weight() + "\t");
   
      for (int i = 0; i < list.size(); i++)
      {
      
         if (i == 0)
         {
         
            System.out.print(" " + list.get(i).start().id());
         
         }
         
         else if (i != list.size() - 1)
         {
         
            System.out.print(" -> " + list.get(i).start().id());
         
         }
         
         else
         {
         
            System.out.print(" -> " + list.get(i).start().id());
            System.out.print(" -> " + list.get(i).end().id());
         
         }
      
      }
   
      System.out.println();
   
   }

   private Chain findBestPathsStartingFrom(Node start, GameMap gameMap)
   {
   
      final Set<Chain> successfulChains = new CopyOnWriteArraySet<>();
   
      startRecursion(start, gameMap, Chain.empty(), successfulChains, gameMap.maxPossibleTraversalWeight());
   
      return
         successfulChains
            .stream()
            .sorted(Chain.comparator)
            .findFirst()
            .orElseThrow()
            ;
   
   }

   private int startRecursion(Node current, GameMap gameMap, Chain chain, Set<Chain> successfulChains, int oldMin)
   {
   
      final ToIntFunction<Future<Integer>> join =  
         eachFuture -> 
         {
            try
            {
                     
               final int result = eachFuture.get();
               
               return result;
                     
            }
                     
            catch (Exception e)
            {
                     
               throw new RuntimeException(e);
                     
            }
         };
   
      final int chainWeight = chain.weight();
   
      successCheck:
      if (chainWeight <= oldMin && gameMap.isSubsetOf(chain))
      {
      
         successfulChains.add(chain);
      
         return chainWeight;
      
      }
   
      oldMin =
         successfulChains
            .stream()
            .mapToInt(Chain::weight)
            .min()
            .orElse(oldMin)
            ;
   
      failCheck:
      if (chainWeight >= oldMin || !chain.shouldAddMore())
      {
      
         return oldMin;
      
      }
   
      final List<Future<Integer>> newMins = new ArrayList<>();
   
      findNextPathLoop:
      for (Path each : gameMap.findPathsFromNodeOptimized(current, chain))
      {
      
         final int currentMin =
            newMins
               .stream()
               .filter(Future::isDone)
               .mapToInt(join)
               .min()
               .orElse(oldMin)
               ;
      
         recursionCall:
         if (chainWeight < currentMin - each.weight() && gameMap.amountOfUntraveledWeightLeft(chain) < currentMin - chainWeight)
         {
         
            newMins
               .add
                  (
                  PARALLEL
                     .submit
                     (
                        () -> 
                           startRecursion
                           (
                              each.end(),
                              gameMap,
                              chain.add(each),
                              successfulChains,
                              currentMin
                           )
                     )
                  );
         
         }
      
         if (currentMin < oldMin)
         {
         
            oldMin = currentMin;
         
         }
      
      }
   
      final int newMin =
         newMins
            .stream()
            .mapToInt(join)
            .min()
            .orElse(oldMin)
            ;
   
      return newMin;
   
   }

}
