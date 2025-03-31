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
}

