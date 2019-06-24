# Guide for publication

1. Create a pull request named `Release vx.y.z` (add the Github tag `release`).

2. Describe the new features and the bug fixes in the [CHANGELOG.md](CHANGELOG.md) file.

3. Upgrade version into `build.gradle`
```grouvy
buildscript {
  ext.lib_version = '4.0.0'
}
```

4. Submit your pull request.

5. Once the branch is merged into `master`, push the new tag.
   
    ```sh
    git push origin <tag_name> 
    ```

    [circleci](https://circleci.com/) will automatically trigger a build, run the tests and publish the new version of the SDK on [`npm`](https://www.npmjs.com/package/@reachfive/identity-core).
    
    > It's important to push the tag separately otherwise the deployement job is not triggered (https://support.circleci.com/hc/en-us/articles/115013854347-Jobs-builds-not-triggered-when-pushing-tag).

    Refer to the [.circleci/config.yml](.circleci/config.yml) file to set up the integration.

6.  Finally, draft a new release in the [Github releases tab](https://github.com/ReachFive/identity-android-sdk/releases) (copy/past the changelog in the release's description).
