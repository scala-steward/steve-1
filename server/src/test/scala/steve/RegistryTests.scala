package steve

import weaver.*
import weaver.scalacheck.Checkers
import cats.effect.IO
import org.scalacheck.Prop.forAll
import Arbitraries.given
import cats.implicits.*

object RegistryTests extends SimpleIOSuite with Checkers:

  val registryR = Registry.inMemory[IO]

  test("save -> lookup returns the same system") {
    forall { (system: SystemState) =>
      registryR.use { registry =>
        for
          hash <- registry.save(system)
          result <- registry.lookup(hash)
        yield assert(result.contains(system))
      }
    }
  }

  test("save is idempotent") {
    forall { (system: SystemState, systems: List[SystemState], hash: Hash) =>
      registryR.use { registry =>
        for
          hash1 <- registry.save(system)
          _ <- systems.traverse_(registry.save)
          hash2 <- registry.save(system)
        yield assert(hash1 == hash2)
      }
    }
  }

  test("lookup is idempotent") {
    forall { (systems: List[SystemState], otherSystems: List[SystemState], hash: Hash) =>
      registryR.use { registry =>
        for
          _ <- systems.traverse_(registry.save)
          result1 <- registry.lookup(hash)
          _ <- otherSystems.traverse_(registry.save)
          result2 <- registry.lookup(hash)
        yield assert(result1 == result2)
      }
    }
  }
