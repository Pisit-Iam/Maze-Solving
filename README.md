# Maze-Solving
The maze is represented as a 2D array of integers, where 0 represents an empty cell, 1 represents a wall. The project uses the BFS algorithm to find the shortest path.
The goal is to see which bfs is the best in terms of performance.

Implementation

* Firstly create structure and direction of the maze
* parallel bfs -- I use Scala’s parallel collections, concurrent data structures, and a ForkJoinPool to efficiently distribute the workload across threads. 

    Steps

        *  Tracks the path from the start (src) to each explored cell.
        *  Ensures each cell is processed only once.
        *  Maintains the current BFS layer for processing.
        *  process each cell in parallel using Scala’s map, 
           where each cell’s neighbors are checked, marked as visited, 
           and added to nextFrontier.
        *  The ForkJoinPool dynamically assigns tasks, 
           allowing threads that finish early to steal work from others, 
           optimizing CPU utilization.
        *  Replace frontier with nextFrontier and repeat until the target cell is reached or all reachable cells are explored.
* shard-based bfs

      Divide the maze into numShards based on rows, and create a 2D parent array to track paths, 
      initializing the source cell as its own parent. Set frontier to the source cell.
      If a neighbor hasn't been visited, assign it a parent and add it to nextFrontier for the next layer. 
      Then replace frontier with next one until we explore all cells


Result
    
    For(1500,1500) Small maze size
    ================ Parallel BFS in Maze ================
    BFS time = 4.9507182 s
    Parent size: 2244004
    ================ Shard Based BFS in Maze ================
    BFS time = 5.9106874 s
    Parent size: 2244004
    ================ Sequence BFS in Maze ================
    BFS time = 13.4124397 s
    Parent size: 2244004

    For(3000,3000) Middle-Large maze size
    ================ Parallel BFS in Maze ================
    BFS time = 35.0413267 s
    Parent size: 8988004
    ================ Shard Based BFS in Maze ================
    BFS time = 32.3637106 s
    Parent size: 8988004
    ================ Sequence BFS in Maze ================
    BFS time = 69.4306832 s
    Parent size: 8988004

    For(4000,4000) Large maze size
    ======== Parallel BFS in Maze =============
    BFS time = 65.7531075 s
    Parent size: 15984004
    ======== Shard Based BFS in Maze ===========
    BFS time = 74.4056801 s
    Parent size: 15984004
    ======== Sequence BFS in Maze ============
    BFS time = 146.5929949 s
    Parent size: 15984004

Discussion
    
    Work stealing is a dynamic load-balancing technique, where idle threads can "steal" work from overloaded workers. 
    This helps ensure that all workers remain busy as long as there is work to be done, 
    preventing any one thread from becoming a bottleneck.
    Shard-based BFS splits the work into disjoint portions and processes them in parallel. 
    While this can help distribute the work, it may also introduce overhead in terms of 
    managing the boundaries between shards.
    The question how could shard-based bfs win in the 3000x3000 which is the mid-large maze
    , but lose to the larger scale. Due to the fact that the larger amount of work in each shard makes Shard-Based BFS more effective 
    because the overhead of managing the shards becomes relatively less significant, and threads can do more meaningful work in parallel.
    However, The work stealing mechanism becomes much more effective in larger maze. As the maze grows, there is more work to distribute across threads. 
    When threads get overloaded, work stealing ensures that idle threads can take over unfinished tasks, leading to better overall load balancing and faster execution.