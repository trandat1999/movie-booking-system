import { eraseSeat, fitToGrid, placeSeat, quickLayout } from './layout-editor';

const SPANS = { STANDARD: 1, VIP: 1, COUPLE: 2 };

describe('layout-editor', () => {
  it('places a couple seat only when the right cell is free and inside the grid', () => {
    const seats = [{ gridRow: 0, gridCol: 1, seatTypeCode: 'STANDARD' }];

    expect(placeSeat(seats, 0, 0, 'COUPLE', SPANS, 4)).toBeNull(); // ô 1 đã có ghế
    expect(placeSeat(seats, 0, 3, 'COUPLE', SPANS, 4)).toBeNull(); // tràn lưới
    expect(placeSeat(seats, 0, 2, 'COUPLE', SPANS, 4)).toEqual([
      ...seats,
      { gridRow: 0, gridCol: 2, seatTypeCode: 'COUPLE' },
    ]);
  });

  it('replaces the type of an existing seat clicked on any of its cells', () => {
    const seats = [{ gridRow: 0, gridCol: 0, seatTypeCode: 'COUPLE' }];

    expect(placeSeat(seats, 0, 1, 'VIP', SPANS, 4)).toEqual([{ gridRow: 0, gridCol: 0, seatTypeCode: 'VIP' }]);
    expect(eraseSeat(seats, 0, 1, SPANS)).toEqual([]);
  });

  it('drops seats that no longer fit after resizing', () => {
    const seats = [
      { gridRow: 0, gridCol: 0, seatTypeCode: 'COUPLE' },
      { gridRow: 2, gridCol: 0, seatTypeCode: 'STANDARD' },
    ];

    expect(fitToGrid(seats, 2, 1, SPANS)).toEqual([]);
    expect(fitToGrid(seats, 3, 2, SPANS)).toEqual(seats);
  });

  it('quick layout adds aisles, VIP rows and a couple last row', () => {
    const layout = quickLayout({ rows: 3, seatsPerRow: 5, aisleAfter: [2], vipFrom: 2, vipTo: 2, coupleLastRow: true });

    expect(layout.gridCols).toBe(6);
    expect(layout.seats.filter((s) => s.gridRow === 0).map((s) => s.gridCol)).toEqual([0, 1, 3, 4, 5]);
    expect(layout.seats.filter((s) => s.gridRow === 1).every((s) => s.seatTypeCode === 'VIP')).toBeTrue();
    expect(layout.seats.filter((s) => s.gridRow === 2).map((s) => s.seatTypeCode)).toEqual(['COUPLE', 'COUPLE', 'STANDARD']);
  });
});
