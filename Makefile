.PHONY: test rust-test go-test android-test server

rust-test:
	cd core/rust && cargo test

go-test:
	cd server/go && go test ./...

android-test:
	cd apps/android && ./gradlew test

test: rust-test go-test

server:
	cd server/go && go run ./cmd/rope-server --config /tmp/rope-dev.json --init --allow-http --listen 127.0.0.1:8443
