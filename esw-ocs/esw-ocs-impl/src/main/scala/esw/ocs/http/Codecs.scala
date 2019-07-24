package esw.ocs.http

import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.http.ProcessSequenceDto._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodecForUnaryCaseClass

trait Codecs extends OcsFrameworkCodecs {
  implicit val successCodec: Codec[Success]                       = deriveCodecForUnaryCaseClass[Success]
  implicit val failureCodec: Codec[Failure]                       = deriveCodecForUnaryCaseClass[Failure]
  implicit val processSequenceDtoCodec: Codec[ProcessSequenceDto] = deriveCodec[ProcessSequenceDto]
}
