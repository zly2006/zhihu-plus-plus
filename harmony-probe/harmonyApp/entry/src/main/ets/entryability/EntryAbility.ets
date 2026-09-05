import { AbilityConstant, ConfigurationConstant, UIAbility, Want } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { window } from '@kit.ArkUI';
import type { Configuration } from '@ohos.app.ability.Configuration';
import nativeApi from 'libentry.so';

const DOMAIN = 0x0000;

export default class EntryAbility extends UIAbility {
  // P1 主壳：把系统深浅色推送给 Compose 侧的 currentSystemInDarkTheme actual。
  private pushColorMode(colorMode?: ConfigurationConstant.ColorMode): void {
    try {
      const mode = colorMode ?? this.context.config?.colorMode;
      nativeApi.applyColorMode(mode === ConfigurationConstant.ColorMode.COLOR_MODE_DARK ? 'dark' : 'light');
    } catch (err) {
      hilog.error(DOMAIN, 'P1', 'Failed to push color mode. Cause: %{public}s', JSON.stringify(err));
    }
  }

  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    try {
      this.context.getApplicationContext().setColorMode(ConfigurationConstant.ColorMode.COLOR_MODE_NOT_SET);
    } catch (err) {
      hilog.error(DOMAIN, 'testTag', 'Failed to set colorMode. Cause: %{public}s', JSON.stringify(err));
    }
    hilog.info(DOMAIN, 'testTag', '%{public}s', 'Ability onCreate');
  }

  onDestroy(): void {
    hilog.info(DOMAIN, 'testTag', '%{public}s', 'Ability onDestroy');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    // Main window is created, set main page for this ability
    hilog.info(DOMAIN, 'testTag', 'Ability onWindowStageCreate');
    this.pushColorMode();

    windowStage.loadContent('pages/Index', (err) => {
      if (err.code) {
        hilog.error(DOMAIN, 'testTag', 'Failed to load the content. Cause: %{public}s', JSON.stringify(err));
        return;
      }
      hilog.info(DOMAIN, 'testTag', 'Succeeded in loading the content.');
    });
  }

  onWindowStageDestroy(): void {
    // Main window is destroyed, release UI related resources
    hilog.info(DOMAIN, 'testTag', '%{public}s', 'Ability onWindowStageDestroy');
  }

  onForeground(): void {
    // Ability has brought to foreground
    hilog.info(DOMAIN, 'testTag', '%{public}s', 'Ability onForeground');
  }

  onBackground(): void {
    // Ability has back to background
    hilog.info(DOMAIN, 'testTag', 'Ability onBackground');
  }

  onConfigurationUpdate(newConfig: Configuration): void {
    hilog.info(DOMAIN, 'P1', 'onConfigurationUpdate colorMode: %{public}s', JSON.stringify(newConfig.colorMode));
    this.pushColorMode(newConfig.colorMode);
  }
  // 返回键拦截不在 UIAbility 层：UIAbility.onBackPressed 不会被系统尊重，
  // 正确的拦截点是 @Entry 组件的 onBackPress()（见 pages/Index.ets）。
}