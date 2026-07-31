#!/bin/bash
# =========================================
# 앱 백그라운드 실행 스크립트
# 배치 구조:
#   /home/jihun/Start.sh, killall.sh
#   /home/jihun/conf/app.conf   ← 포트 등 환경 설정
#   /home/jihun/libs/message-project-0.0.1-SNAPSHOT.jar
#   /home/jihun/logs/app.log    ← 당일 로그 (자정에 YYYY-MM-DD.log로 자동 보관)
# 사용법: ./Start.sh
# =========================================

# 스크립트가 있는 위치를 기준 경로로 사용 (어디서 실행해도 동일하게 동작)
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

JAR_NAME="message-project-0.0.1-SNAPSHOT.jar"
APP_JAR="$BASE_DIR/libs/$JAR_NAME"
CONF_FILE="$BASE_DIR/conf/app.conf"
LOG_DIR="$BASE_DIR/logs"

mkdir -p "$LOG_DIR"

# 1. 설정 파일 로드 (SERVER_PORT 등)
if [ -f "$CONF_FILE" ]; then
  source "$CONF_FILE"
else
  echo "[WARN] 설정 파일이 없습니다: $CONF_FILE → 기본값 사용"
fi
: "${SERVER_PORT:=8080}"   # 설정 파일에 없으면 기본값 8080

# 2. jar 파일 존재 확인
if [ ! -f "$APP_JAR" ]; then
  echo "[ERROR] jar 파일이 없습니다: $APP_JAR"
  echo "        WinSCP로 libs/ 폴더에 $JAR_NAME 을 업로드하세요."
  exit 1
fi

# 3. 이미 실행 중인지 확인 (중복 실행 방지)
PID=$(pgrep -f "java -jar $APP_JAR")
if [ -n "$PID" ]; then
  echo "[WARN] 이미 실행 중입니다. (PID: $PID)"
  echo "       재시작하려면 먼저 ./killall.sh 로 종료하세요."
  exit 1
fi

# 4. 백그라운드 실행
#    - 애플리케이션 로그: logback이 $LOG_DIR/app.log 에 기록하고 매일 자정에 날짜 파일로 보관
#    - console.log: JVM 기동 실패 등 logback이 뜨기 전의 출력만 별도 기록
echo "[INFO] 앱 시작 (포트: $SERVER_PORT, 로그: $LOG_DIR/app.log)"
cd "$BASE_DIR"
SERVER_PORT="$SERVER_PORT" LOG_DIR="$LOG_DIR" nohup java -jar "$APP_JAR" > "$LOG_DIR/console.log" 2>&1 &

echo "[INFO] 시작 완료 (PID: $!)"
echo "[INFO] 실시간 로그 보기: tail -f $LOG_DIR/app.log"
