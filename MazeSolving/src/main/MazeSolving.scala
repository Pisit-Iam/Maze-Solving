import scala.collection.parallel.CollectionConverters._
import scala.annotation.tailrec
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.concurrent.TrieMap
import java.util.concurrent.ForkJoinPool
import scala. collection. JavaConverters. asScalaIteratorConverter

object MazeSolving {

  type Maze = Array[Array[Int]]
  type Parent = TrieMap[Cell, Cell]
  type Parent1 = Array[Array[Option[(Int, Int)]]]

  val directions: Seq[(Int, Int)] = List(
    (0, 1), // Right
    (1, 0), // Down
    (0, -1), // Left
    (-1, 0) // Up
  )

  case class Cell(x: Int, y: Int)

  // Return available neighbor cells that are within bounds and not blocked (maze cell == 0)
  private def neighbors(cell: Cell, maze: Maze): Seq[Cell] = {
    def inBounds(x: Int, y: Int): Boolean =
      x >= 0 && x < maze.length && y >= 0 && y < maze(0).length
    directions.flatMap { case (dx, dy) =>
      val (nx, ny) = (cell.x + dx, cell.y + dy)
      if (inBounds(nx, ny) && maze(nx)(ny) == 0) Some(Cell(nx, ny)) else None
    }
  }

  def seqBFS(src: Cell, maze: Maze): Map[Cell, Cell] = {
    @tailrec
    def iterate(frontier: Set[Cell], visited: Set[Cell], parent: Map[Cell, Cell]): Map[Cell, Cell] = {
      if (frontier.isEmpty) parent else {
        val visited_ = visited ++ frontier
        val newParent = frontier.flatMap { v =>
          neighbors(v, maze).filterNot(visited_).map(neighbor => neighbor -> v)
        }.toMap
        iterate(newParent.keySet, visited_, parent ++ newParent)
      }
    }

    iterate(Set(src), Set(), Map(src -> src))
  }

  def parallelBFS(src: Cell, dst: Cell, maze: Maze): Parent = {
    val parent = TrieMap[Cell, Cell]()
    val visited = TrieMap[Cell, Boolean]()
    val frontier = new ConcurrentLinkedQueue[Cell]()

    val pool = new ForkJoinPool() //execution pool
    frontier.add(src)
    parent.put(src, src)
    visited.put(src, true)

    while (frontier.nonEmpty && !parent.contains(dst)) {
      val nextFrontier = new ConcurrentLinkedQueue[Cell]()
      val currentFrontierList = frontier.iterator().asScala.toList.par
      currentFrontierList.foreach { cell =>
        for (neighbor <- neighbors(cell, maze)) {
          if (visited.putIfAbsent(neighbor, true).isEmpty) {
            parent.put(neighbor, cell) // Record the parent (i.e. discoverer) of the neighbor.
            nextFrontier.add(neighbor) // add to the next layer
          }
        }
      }
      // let move to the next layer
      frontier.clear()
      frontier.addAll(nextFrontier)
    }
    parent
  }

  def shardBFS(src: Cell, dst: Cell, maze: Maze, numShards: Int = 4): Parent1 = {
    val rows = maze.length
    val cols = maze(0).length
    val shardSize = rows / numShards

    val parent = Array.fill(rows, cols)(Option.empty[(Int, Int)])
    parent(src.x)(src.y) = Some((src.x, src.y))

    val shards = (0 until numShards).map { i =>
      val startRow = i * shardSize
      val endRow = if (i == numShards - 1) rows else (i + 1) * shardSize
      (startRow, endRow)
    }

    var frontier = Set(src)

    while (frontier.nonEmpty) {
      val nextFrontier = new ConcurrentLinkedQueue[Cell]()

      shards.par.foreach { case (startRow, endRow) =>
        frontier.foreach { cell =>
          if (cell.x >= startRow && cell.x < endRow) {
            for (neighbor <- neighbors(cell, maze)) {
              if (parent(neighbor.x)(neighbor.y).isEmpty) {
                parent(neighbor.x)(neighbor.y) = Some((cell.x, cell.y))
                nextFrontier.add(neighbor)
              }
            }
          }
        }
      }
      frontier = Iterator.continually(nextFrontier.poll()).takeWhile(_ != null).toSet
    }
    parent
  }

  def aWayOut(parent: Parent, src: Cell, dst: Cell): Option[List[Cell]] = {
    @tailrec
    def path(cur: Cell, acc: List[Cell]): List[Cell] = {
      if (cur == src) cur :: acc else path(parent(cur), cur :: acc)
    }

    if (parent.contains(dst)) Some(path(dst, Nil)) else None
  }
}

