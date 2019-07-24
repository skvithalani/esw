package esw.ocs.restless

import csw.params.core.formats.{CodecHelpers, ParamCodecs}
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.api.models.messages.error.StepListError
import esw.ocs.api.models.messages.error.StepListError._
import esw.ocs.restless.SeqMsg._
import io.bullet.borer.{Codec, Decoder, Encoder}
import io.bullet.borer.derivation.MapBasedCodecs._
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodecForUnaryCaseClass

trait SeqMsgCodecs extends ParamCodecs with OcsFrameworkCodecs {
  def singletonCodec2[T <: Singleton](a: T): Codec[T] = CodecHelpers.bimap[Map[String, String], T](_ => a, Map.empty)

  implicit lazy val SeqMsgCodec: Codec[SeqMsg] = deriveCodec[SeqMsg]

  implicit def SeqeucerSignalsCodec[T <: SeqSignal]: Codec[T] = {
    implicit lazy val ShutdownCodec: Codec[Shutdown.type] = singletonCodec(Shutdown)
    implicit lazy val AbortCodec: Codec[Abort.type]       = singletonCodec(Abort)
    deriveCodec[SeqSignal].asInstanceOf[Codec[T]]
  }

  implicit def EditorMessagesCodec[T <: EditorMsg]: Codec[T] = {
    implicit lazy val AvailableCodec: Codec[Available.type]                     = singletonCodec(Available)
    implicit lazy val GetSequenceCodec: Codec[GetSequence.type]                 = singletonCodec(GetSequence)
    implicit lazy val GetPreviousSequenceCodec: Codec[GetPreviousSequence.type] = singletonCodec(GetPreviousSequence)
    implicit lazy val AddCodec: Codec[Add]                                      = deriveCodec[Add]
    implicit lazy val PrependCodec: Codec[Prepend]                              = deriveCodec[Prepend]
    implicit lazy val ReplaceCodec: Codec[Replace]                              = deriveCodec[Replace]
    implicit lazy val InsertAfterCodec: Codec[InsertAfter]                      = deriveCodec[InsertAfter]
    implicit lazy val DeleteCodec: Codec[Delete]                                = deriveCodec[Delete]
    implicit lazy val AddBreakpointCodec: Codec[AddBreakpoint]                  = deriveCodec[AddBreakpoint]
    implicit lazy val RemoveBreakpointCodec: Codec[RemoveBreakpoint]            = deriveCodec[RemoveBreakpoint]
    implicit lazy val PauseCodec: Codec[Pause.type]                             = singletonCodec(Pause)
    implicit lazy val ResumeCodec: Codec[Resume.type]                           = singletonCodec(Resume)
    implicit lazy val ResetCodec: Codec[Reset.type]                             = singletonCodec(Reset)

    deriveCodec[EditorMsg].asInstanceOf[Codec[T]]
  }

  implicit lazy val StepListErrorCodec: Codec[StepListError]                 = deriveCodec[StepListError]
  implicit lazy val PauseErrorCodec: Codec[PauseError]                       = deriveCodec[PauseError]
  implicit lazy val AddErrorCodec: Codec[AddError]                           = deriveCodec[AddError]
  implicit lazy val AddBreakpointErrorCodec: Codec[AddBreakpointError]       = deriveCodec[AddBreakpointError]
  implicit lazy val ResumeErrorCodec: Codec[ResumeError]                     = deriveCodec[ResumeError]
  implicit lazy val PrependErrorCodec: Codec[PrependError]                   = deriveCodec[PrependError]
  implicit lazy val ResetErrorCodec: Codec[ResetError]                       = deriveCodec[ResetError]
  implicit lazy val InsertErrorCodec: Codec[InsertError]                     = deriveCodec[InsertError]
  implicit lazy val ReplaceErrorCodec: Codec[ReplaceError]                   = deriveCodec[ReplaceError]
  implicit lazy val DeleteErrorCodec: Codec[DeleteError]                     = deriveCodec[DeleteError]
  implicit lazy val RemoveBreakpointErrorCodec: Codec[RemoveBreakpointError] = deriveCodec[RemoveBreakpointError]

//  def ethrCodec[A: Encoder: Decoder, B: Encoder: Decoder]: Codec[Either[A, B]] = {
//    implicit val leftCodec: Codec[Left[A, B]]   = deriveCodecForUnaryCaseClass[Left[A, B]]
//    implicit val rightCodec: Codec[Right[A, B]] = deriveCodecForUnaryCaseClass[Right[A, B]]
//    deriveCodec[Either[A, B]]
//  }
//  implicit def ethrEnc[A: Encoder: Decoder, B: Encoder: Decoder]: Encoder[Either[A, B]] = ethrCodec[A, B].encoder
//  implicit def ethrDec[A: Encoder: Decoder, B: Encoder: Decoder]: Decoder[Either[A, B]] = ethrCodec[A, B].decoder
}
