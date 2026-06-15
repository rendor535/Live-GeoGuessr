import {
  getFriendRequestId,
} from "../../src/shared/firestore";

describe("getFriendRequestId", () => {
  it("sorts UIDs and joins them", () => {
    const result = getFriendRequestId(
      "user-b",
      "user-a",
    );

    expect(result).toBe(
      "user-a_user-b",
    );
  });

  it("returns the same ID regardless of argument order", () => {
    const first = getFriendRequestId(
      "user-a",
      "user-b",
    );

    const second = getFriendRequestId(
      "user-b",
      "user-a",
    );

    expect(first).toBe(second);
  });

  it("creates deterministic ID", () => {
    expect(
      getFriendRequestId(
        "abc",
        "xyz",
      ),
    ).toBe("abc_xyz");
  });
});