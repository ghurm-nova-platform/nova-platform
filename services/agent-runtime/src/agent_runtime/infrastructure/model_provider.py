"""Provider-neutral model adapter stubs (no vendor SDKs)."""

from __future__ import annotations

from agent_runtime.domain.interfaces import ModelProviderPort
from agent_runtime.shared.exceptions import ValidationAppError


class NullModelProvider(ModelProviderPort):
    """
    Placeholder model provider used until a real adapter is wired.

    Ensures the runtime boots without binding to a commercial AI vendor.
    """

    def provider_name(self) -> str:
        """Return the null adapter name."""

        return "null"

    def complete(self, *, prompt: str, model_class: str, max_tokens: int) -> str:
        """Refuse completion until a concrete provider is configured."""

        raise ValidationAppError(
            "No model provider configured. Implement ModelProviderPort "
            f"(requested model_class={model_class}, max_tokens={max_tokens})."
        )
