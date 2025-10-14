import sys
import io
import logging
import warnings

# 로그 파일을 설정합니다.
logging.basicConfig(filename="app.log", level=logging.WARNING)

# 경고를 로그 파일로만 기록하도록 설정
logging.captureWarnings(True)
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8') # 한글 깨짐 방지
from faster_whisper import WhisperModel # faster-whisper 라이브러리에서 WhisperModel 클래스를 가져옴.

if len(sys.argv) < 2:  # 오디오 파일 경로가 없을 경우
    print("No audio file provided")
    sys.exit(1)

audio_presigned_url = sys.argv[1] # 명령줄에서 입력받은 오디오 파일 경로를 변수 audio_path에 저장.

# CPU
model = WhisperModel("large-v3", device="cpu", compute_type="int8") # 모델 종류, CPU 지정

segments, info = model.transcribe(audio_presigned_url, language="ko")
# model.transcribe: 실제 음성을 텍스트로 변환하는 함수.
# audio_path의 음성을 불러와 한국어로 인식하도록 설정.

# 전사 결과 합치기
transcript = " ".join([seg.text for seg in segments])
print(transcript)
