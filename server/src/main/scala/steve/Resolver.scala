package steve

import cats.effect.implicits.*
import cats.implicits.*
import cats.MonadThrow
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import steve.Build.Error.*
import cats.effect.std.UUIDGen

trait Resolver[F[_]]:
  def resolve(build: Build): F[ResolvedBuild]

object Resolver:

  def apply[F[_]](using ev: Resolver[F]) = ev

  def instance[F[_]: MonadThrow: Registry]: Resolver[F] =
    new Resolver {

      private val resolveCommand: Build.Command => ResolvedBuild.Command =
        case Build.Command.Upsert(k, v) => ResolvedBuild.Command.Upsert(k, v)
        case Build.Command.Delete(k)    => ResolvedBuild.Command.Delete(k)

      private def resolveBase(base: Build.Base): F[SystemState] =
        base match
          case Build.Base.EmptyImage =>
            Registry[F]
              .lookup(Registry.emptyHash)
              .flatMap(_.liftTo[F](Throwable("Impossible! Hash not found for emptyImage")))
          case Build.Base.ImageReference(hash) =>
            Registry[F]
              .lookup(hash)
              .flatMap(_.liftTo[F](UnknownBase(hash)))

      def resolve(build: Build): F[ResolvedBuild] = resolveBase(build.base)
        .map { sys =>
          ResolvedBuild(sys, build.commands.map(resolveCommand))
        }

    }
