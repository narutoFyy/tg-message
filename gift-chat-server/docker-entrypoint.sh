#!/bin/sh
set -eu

image_dir="${APP_UPLOAD_IMAGE_DIR:-/var/lib/gift-chat/uploads/images}"
voice_dir="${APP_UPLOAD_VOICE_DIR:-/var/lib/gift-chat/uploads/voices}"
video_dir="${APP_UPLOAD_VIDEO_DIR:-/var/lib/gift-chat/uploads/videos}"

for upload_dir in "$image_dir" "$voice_dir" "$video_dir"; do
    mkdir -p "$upload_dir"
    chown giftchat:giftchat "$upload_dir"
done

exec setpriv --reuid=giftchat --regid=giftchat --init-groups \
    /opt/java/openjdk/bin/java -jar /app/app.jar
