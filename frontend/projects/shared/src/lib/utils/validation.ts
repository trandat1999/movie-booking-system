import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Khớp với PhoneNumbers.VN_MOBILE_PATTERN ở backend. */
export const VN_MOBILE_PATTERN = /^(0|\+84)(3|5|7|8|9)\d{8}$/;

/** Khớp với PasswordPolicy ở backend: 8–64 ký tự, có chữ và số. */
export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/;
export const PASSWORD_HINT = 'Mật khẩu dài 8–64 ký tự, gồm cả chữ và số';

/** Validator cho form group: hai control phải bằng nhau (ví dụ nhập lại mật khẩu). */
export function matchFields(field: string, confirmField: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const confirm = group.get(confirmField);
    if (!confirm) {
      return null;
    }
    const errors: ValidationErrors = { ...(confirm.errors ?? {}) };
    delete errors['mismatch'];
    if (confirm.value && group.get(field)?.value !== confirm.value) {
      errors['mismatch'] = true;
    }
    confirm.setErrors(Object.keys(errors).length ? errors : null);
    return null;
  };
}
