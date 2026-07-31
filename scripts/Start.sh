#!/bin/bash
# =========================================
# 앱 백그라운드 실행 스크립트
# 사용법: ./Start.sh
# =========================================

APP_JAR="app.jar"     # 실행할 jar 파일명
LOG_FILE="app.log"    # 로그 파일
PORT=8080             # 서비스 포트 (EC2 보안그룹에 열려있는 포트)

# 1. jar 파일 존재 확인
if [ ! -f "$APP_JAR" ]; then
  echo "[ERROR] $APP_JAR 파일이 없습니다. 먼저 jar를 업로드하세요."
  exit 1
fi

# 2. 이미 실행 중인지 확인 (중복 실행 방지)
PID=$(pgrep -f "java -jar $APP_JAR")
if [ -n "$PID" ]; then
  echo "[WARN] 이미 실행 중입니다. (PID: $PID)"
  echo "       재시작하려면 먼저 ./killall.sh 로 종료하세요."
  exit 1
fi

# 3. 백그라운드 실행
#    nohup : SSH 접속을 끊어도 프로세스 유지
#    &     : 백그라운드로 실행
#    > $LOG_FILE 2>&1 : 표준출력/에러를 로그 파일로 기록
echo "[INFO] 앱 시작 (포트: $PORT)"
SERVER_PORT=$PORT nohup java -jar "$APP_JAR" > "$LOG_FILE" 2>&1 &

echo "[INFO] 시작 완료 (PID: $!)"
echo "[INFO] 실시간 로그 보기: tail -f $LOG_FILE"
