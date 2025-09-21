import sys
from faster_whisper import WhisperModel

if len(sys.argv) < 2:
    print("No audio file provided")
    sys.exit(1)

audio_path = sys.argv[1]

# CPU 모드로 모델 로드
model = WhisperModel("large-v3", device="cpu", compute_type="int8")

segments, info = model.transcribe(audio_path, language="ko")

# 전사 결과 합치기
transcript = " ".join([seg.text for seg in segments])
print(transcript)
