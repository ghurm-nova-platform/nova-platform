import { FormControl } from '@angular/forms';

import {
  credentialReferenceValidator,
  isValidCredentialReference,
  looksLikePlaintextSecret,
} from './credential-reference.validator';

describe('credentialReferenceValidator', () => {
  it('accepts empty values and env / vault references', () => {
    expect(isValidCredentialReference('')).toBeTrue();
    expect(isValidCredentialReference(null)).toBeTrue();
    expect(isValidCredentialReference('env:NOVA_PROVIDER_OPENAI')).toBeTrue();
    expect(
      isValidCredentialReference('vault:provider-secret:aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01'),
    ).toBeTrue();
  });

  it('rejects raw API keys and other plaintext secret shapes', () => {
    expect(looksLikePlaintextSecret('sk-abcdefghijklmnopqrstuvwxyz123456')).toBeTrue();
    expect(isValidCredentialReference('sk-abcdefghijklmnopqrstuvwxyz123456')).toBeFalse();
    expect(isValidCredentialReference('Bearer abc.def.ghi')).toBeFalse();
    expect(isValidCredentialReference('api_key=super-secret')).toBeFalse();
    expect(isValidCredentialReference('{"api_key":"secret"}')).toBeFalse();
    expect(isValidCredentialReference('not-a-valid-reference')).toBeFalse();
  });

  it('wires as an Angular form validator without treating references as API key storage', () => {
    const control = new FormControl('', { validators: [credentialReferenceValidator()] });
    expect(control.valid).toBeTrue();

    control.setValue('vault:provider-secret:aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01');
    expect(control.valid).toBeTrue();

    control.setValue('sk-abcdefghijklmnopqrstuvwxyz123456');
    expect(control.hasError('credentialReference')).toBeTrue();
  });
});
