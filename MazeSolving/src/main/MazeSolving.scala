import scala.collection.parallel.CollectionConverters._
import scala.annotation.tailrec
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.concurrent.TrieMap

object MazeSolving {

  type Maze = Array[Array[Int]]
  type Parent = TrieMap[Cell, Cell]

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
    var frontier = Set(src)

    parent.put(src, src)
    visited.put(src, true)

    // Process BFS layers until the frontier is empty or destination is found.
    while (frontier.nonEmpty && !parent.contains(dst)) {
      val nextFrontierQueue = new ConcurrentLinkedQueue[Cell]()
      frontier.par.foreach { cell =>
        for (neighbor <- neighbors(cell, maze)) {
          if (visited.putIfAbsent(neighbor, true).isEmpty) {
            parent.put(neighbor, cell) // Record the parent (i.e. discoverer) of the neighbor.
            nextFrontierQueue.add(neighbor)
          }
        }
      }
      // Update frontier with all newly discovered cells.
      frontier = Iterator.continually(nextFrontierQueue.poll()).takeWhile(_ != null).toSet
    }
    parent
  }
}

