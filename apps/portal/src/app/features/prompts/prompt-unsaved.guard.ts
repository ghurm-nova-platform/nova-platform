import { CanDeactivateFn } from '@angular/router';

export interface PromptFormCanDeactivate {
  hasUnsavedChanges(): boolean;
}

export const promptUnsavedGuard: CanDeactivateFn<PromptFormCanDeactivate> = (component) => {
  if (!component.hasUnsavedChanges()) {
    return true;
  }
  return window.confirm('You have unsaved changes. Leave without saving?');
};
