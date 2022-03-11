export VERSION=$(./gradlew conductor-workflow-event-listener:dependencies | grep conductor-core | grep ">" | tail -1 | cut -d ' ' -f4)
echo $VERSION
git tag v$VERSION

