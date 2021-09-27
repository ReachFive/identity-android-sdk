# Guide for publication

1. Create a pull request named `Release vx.y.z` (add the Github tag `release`).

2. Describe the new features and the bug fixes in the [CHANGELOG.md](CHANGELOG.md) file.

3. Upgrade the `x.y.z` version into `libversion.gradle`:

```groovy
ext.libversion="x.y.z"
```

4. Submit your pull request.

5. Once the branch is merged into `master`, create a new tag.

    ```sh
    git tag <tag_name> 
    ```
    
    Push it on origin.
   
    ```sh
    git push origin <tag_name> 
    ```

    [circleci](https://circleci.com/gh/ReachFive/identity-android-sdk) will automatically trigger a build, run the tests and publish the new version of the SDK on [`MavenCentral`](https://search.maven.org/search?q=g:co.reachfive.identity).
    
    > It's important to push the tag separately otherwise the deployment job is not triggered (https://support.circleci.com/hc/en-us/articles/115013854347-Jobs-builds-not-triggered-when-pushing-tag).

    Refer to the [.circleci/config.yml](.circleci/config.yml) file to set up the integration.

6.  Finally, create a new release in the [Github releases tab](https://github.com/ReachFive/identity-android-sdk/releases) (copy & paste the changelog in the release's description).
